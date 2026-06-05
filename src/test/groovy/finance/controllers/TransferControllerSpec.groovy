package finance.controllers

import finance.domain.Transfer
import finance.exceptions.DuplicateTransferException
import finance.services.TransferService
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import spock.lang.Specification

import java.math.BigDecimal
import java.time.LocalDate

@SuppressWarnings("GroovyAccessibility")
class TransferControllerSpec extends Specification {

    private TransferService transferServiceMock = GroovyMock(TransferService)
    private TransferController controller = new TransferController(transferServiceMock)

    private Transfer buildTransfer(Long id = 1L) {
        Transfer transfer = new Transfer()
        transfer.transferId = id
        transfer.sourceAccount = 'checking_brian'
        transfer.destinationAccount = 'savings_brian'
        transfer.transactionDate = LocalDate.of(2020, 12, 1)
        transfer.amount = new BigDecimal('100.00')
        transfer.guidSource = '4ea3be58-3993-abcd-88a2-4ffc7f1d73bd'
        transfer.guidDestination = '5fb4cf69-4aa4-bcde-99b3-5ggd8g2e84ce'
        return transfer
    }

    void 'test selectAllTransfers - returns 200 with transfers'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        HttpResponse response = controller.selectAllTransfers()

        then:
        response.status == HttpStatus.OK
        1 * transferServiceMock.findAllTransfers() >> [transfer]
        0 * _
    }

    void 'test selectAllTransfers - returns 200 with empty list'() {
        when:
        HttpResponse response = controller.selectAllTransfers()

        then:
        response.status == HttpStatus.OK
        1 * transferServiceMock.findAllTransfers() >> []
        0 * _
    }

    void 'test insertTransfer - returns 201 on success'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        HttpResponse response = controller.insertTransfer(transfer)

        then:
        response.status == HttpStatus.CREATED
        1 * transferServiceMock.insertTransfer(transfer) >> transfer
        0 * _
    }

    void 'test insertTransfer - throws 409 on DuplicateTransferException'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        controller.insertTransfer(transfer)

        then:
        HttpStatusException ex = thrown()
        ex.status == HttpStatus.CONFLICT
        1 * transferServiceMock.insertTransfer(transfer) >> { throw new DuplicateTransferException('duplicate') }
        0 * _
    }

    void 'test insertTransfer - throws 400 on RuntimeException'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        controller.insertTransfer(transfer)

        then:
        HttpStatusException ex = thrown()
        ex.status == HttpStatus.BAD_REQUEST
        1 * transferServiceMock.insertTransfer(transfer) >> { throw new RuntimeException('source not found') }
        0 * _
    }

    void 'test selectByTransferId - returns 200 when found'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        HttpResponse response = controller.selectByTransferId(1L)

        then:
        response.status == HttpStatus.OK
        1 * transferServiceMock.findByTransferId(1L) >> Optional.of(transfer)
        0 * _
    }

    void 'test selectByTransferId - returns 404 when not found'() {
        when:
        HttpResponse response = controller.selectByTransferId(999L)

        then:
        response.status == HttpStatus.NOT_FOUND
        1 * transferServiceMock.findByTransferId(999L) >> Optional.empty()
        0 * _
    }

    void 'test updateTransfer - returns 200 on success'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        HttpResponse response = controller.updateTransfer(1L, transfer)

        then:
        response.status == HttpStatus.OK
        1 * transferServiceMock.updateTransfer(1L, transfer) >> Optional.of(transfer)
        0 * _
    }

    void 'test updateTransfer - throws 404 when not found'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        controller.updateTransfer(999L, transfer)

        then:
        HttpStatusException ex = thrown()
        ex.status == HttpStatus.NOT_FOUND
        1 * transferServiceMock.updateTransfer(999L, transfer) >> Optional.empty()
        0 * _
    }

    void 'test deleteByTransferId - returns 200 when found'() {
        given:
        Transfer transfer = buildTransfer()

        when:
        HttpResponse response = controller.deleteByTransferId(1L)

        then:
        response.status == HttpStatus.OK
        1 * transferServiceMock.findByTransferId(1L) >> Optional.of(transfer)
        1 * transferServiceMock.deleteByTransferId(1L)
        0 * _
    }

    void 'test deleteByTransferId - throws 404 when not found'() {
        when:
        controller.deleteByTransferId(999L)

        then:
        HttpStatusException ex = thrown()
        ex.status == HttpStatus.NOT_FOUND
        1 * transferServiceMock.findByTransferId(999L) >> Optional.empty()
        0 * _
    }
}
