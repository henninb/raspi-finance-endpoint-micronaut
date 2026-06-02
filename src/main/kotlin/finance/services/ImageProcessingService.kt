package finance.services

import finance.domain.ImageFormatType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.coobird.thumbnailator.Thumbnails
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader

@Singleton
open class ImageProcessingService(
    @Inject private val meterService: MeterService,
) : IImageProcessingService {

    companion object {
        private val logger = LogManager.getLogger()
        private const val THUMBNAIL_SIZE = 100
        private const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    }

    override fun createThumbnail(rawImage: ByteArray, imageFormatType: ImageFormatType): ByteArray {
        return try {
            if (rawImage.isEmpty()) {
                logger.warn("Cannot create thumbnail from empty image data")
                meterService.incrementExceptionCaughtCounter("EmptyImageData")
                return byteArrayOf()
            }
            if (imageFormatType == ImageFormatType.Undefined) {
                logger.warn("Cannot create thumbnail for undefined image format")
                meterService.incrementExceptionCaughtCounter("UndefinedImageFormat")
                return byteArrayOf()
            }
            val bufferedImage = ImageIO.read(ByteArrayInputStream(rawImage))
            if (bufferedImage == null) {
                logger.warn("Could not read image data for thumbnail creation")
                meterService.incrementExceptionCaughtCounter("ImageReadFailure")
                return byteArrayOf()
            }
            val thumbnail = Thumbnails.of(bufferedImage).size(THUMBNAIL_SIZE, THUMBNAIL_SIZE).asBufferedImage()
            val out = ByteArrayOutputStream()
            val writeSuccess = ImageIO.write(thumbnail, imageFormatType.toString(), out)
            if (!writeSuccess) {
                logger.warn("Failed to write thumbnail image in format: $imageFormatType")
                meterService.incrementExceptionCaughtCounter("ThumbnailWriteFailure")
                return byteArrayOf()
            }
            val thumbnailBytes = out.toByteArray()
            logger.info("Created thumbnail: ${thumbnailBytes.size} bytes from original ${rawImage.size} bytes")
            meterService.incrementExceptionThrownCounter("ThumbnailCreated")
            thumbnailBytes
        } catch (ioe: IIOException) {
            logger.warn("IIOException during thumbnail creation: ${ioe.message}")
            meterService.incrementExceptionCaughtCounter("IIOException")
            byteArrayOf()
        } catch (ex: Exception) {
            logger.error("Unexpected error during thumbnail creation: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ThumbnailCreationError")
            byteArrayOf()
        }
    }

    override fun getImageFormatType(rawImage: ByteArray): ImageFormatType {
        return try {
            val imageInputStream = ImageIO.createImageInputStream(ByteArrayInputStream(rawImage))
                ?: run {
                    logger.warn("Failed to create ImageInputStream for format detection")
                    meterService.incrementExceptionCaughtCounter("ImageInputStreamCreationFailed")
                    return ImageFormatType.Undefined
                }
            try {
                val readers: Iterator<ImageReader> = ImageIO.getImageReaders(imageInputStream)
                var format = ImageFormatType.Undefined
                readers.forEachRemaining { reader ->
                    format = when (reader.formatName.lowercase()) {
                        "jpeg" -> { logger.info("Detected image format: ${reader.formatName}"); ImageFormatType.Jpeg }
                        "png" -> { logger.info("Detected image format: ${reader.formatName}"); ImageFormatType.Png }
                        else -> { logger.debug("Unsupported format: ${reader.formatName}"); ImageFormatType.Undefined }
                    }
                }
                format
            } finally {
                try { imageInputStream.close() } catch (ex: Exception) { logger.warn("Failed to close ImageInputStream: ${ex.message}") }
            }
        } catch (ex: Exception) {
            logger.error("Error detecting image format: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ImageFormatDetectionError")
            ImageFormatType.Undefined
        }
    }

    override fun validateImageSize(rawImage: ByteArray): Boolean {
        return try {
            val isValid = rawImage.isNotEmpty() && rawImage.size <= MAX_IMAGE_SIZE_BYTES
            if (!isValid) {
                logger.warn("Image validation failed: size=${rawImage.size}, max=$MAX_IMAGE_SIZE_BYTES")
                meterService.incrementExceptionCaughtCounter("ImageSizeValidationFailed")
            }
            isValid
        } catch (ex: Exception) {
            logger.error("Error during image size validation: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ImageValidationError")
            false
        }
    }

    override fun processImage(rawImage: ByteArray): ImageProcessingResult {
        return try {
            val format = getImageFormatType(rawImage)
            val isValid = validateImageSize(rawImage) && format != ImageFormatType.Undefined
            val thumbnail = if (isValid) createThumbnail(rawImage, format) else byteArrayOf()
            val result = ImageProcessingResult(
                format = format,
                thumbnail = thumbnail,
                isValid = isValid,
                originalSize = rawImage.size,
                thumbnailSize = thumbnail.size,
            )
            logger.info("Image processing completed: format=$format, valid=$isValid, original=${result.originalSize}, thumbnail=${result.thumbnailSize}")
            meterService.incrementExceptionThrownCounter("ImageProcessingCompleted")
            result
        } catch (ex: Exception) {
            logger.error("Error during image processing: ${ex.message}", ex)
            meterService.incrementExceptionCaughtCounter("ImageProcessingError")
            ImageProcessingResult(ImageFormatType.Undefined, byteArrayOf(), false, rawImage.size, 0)
        }
    }
}
