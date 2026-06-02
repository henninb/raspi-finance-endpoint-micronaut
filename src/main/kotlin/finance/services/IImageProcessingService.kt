package finance.services

import finance.domain.ImageFormatType

interface IImageProcessingService {
    fun createThumbnail(rawImage: ByteArray, imageFormatType: ImageFormatType): ByteArray
    fun getImageFormatType(rawImage: ByteArray): ImageFormatType
    fun validateImageSize(rawImage: ByteArray): Boolean
    fun processImage(rawImage: ByteArray): ImageProcessingResult
}

data class ImageProcessingResult(
    val format: ImageFormatType,
    val thumbnail: ByteArray,
    val isValid: Boolean,
    val originalSize: Int,
    val thumbnailSize: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageProcessingResult
        if (format != other.format) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false
        if (isValid != other.isValid) return false
        if (originalSize != other.originalSize) return false
        if (thumbnailSize != other.thumbnailSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + thumbnail.contentHashCode()
        result = 31 * result + isValid.hashCode()
        result = 31 * result + originalSize
        result = 31 * result + thumbnailSize
        return result
    }
}
