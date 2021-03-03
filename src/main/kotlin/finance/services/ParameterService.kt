package finance.services

import finance.repositories.ParameterRepository

import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@Singleton
class ParameterService(@Inject val parameterRepository: ParameterRepository, @Inject val validator: Validator, @Inject val meterService: MeterService) {
}