package finance.graphql

import graphql.GraphQL
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.language.Value
import graphql.scalars.ExtendedScalars
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.core.io.ResourceResolver
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timestampDateTimeScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("DateTime")
    .description("DateTime scalar handling java.sql.Timestamp and java.time.OffsetDateTime")
    .coercing(object : Coercing<OffsetDateTime, String> {
        override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String =
            when (dataFetcherResult) {
                is Timestamp -> dataFetcherResult.toInstant().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                is OffsetDateTime -> dataFetcherResult.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                else -> throw CoercingSerializeException(
                    "Expected Timestamp or OffsetDateTime but was '${dataFetcherResult.javaClass}'"
                )
            }

        override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): OffsetDateTime =
            when (input) {
                is String -> OffsetDateTime.parse(input, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                is OffsetDateTime -> input
                else -> throw CoercingParseValueException("Expected a String but was '${input.javaClass}'")
            }

        override fun parseLiteral(
            input: Value<*>,
            variables: CoercedVariables,
            graphQLContext: GraphQLContext,
            locale: Locale,
        ): OffsetDateTime =
            when (input) {
                is StringValue -> OffsetDateTime.parse(input.value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                else -> throw CoercingParseLiteralException("Expected a StringValue but was '${input.javaClass}'")
            }
    })
    .build()

private val idScalar: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("ID")
    .description("ID scalar coerced to Long for numeric identifiers")
    .coercing(object : Coercing<Long, String> {
        override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String =
            dataFetcherResult.toString()

        override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Long =
            when (input) {
                is Long -> input
                is Int -> input.toLong()
                is String -> input.toLongOrNull()
                    ?: throw CoercingParseValueException("Cannot coerce '$input' to Long")
                else -> throw CoercingParseValueException("Expected String or Int but was '${input.javaClass}'")
            }

        override fun parseLiteral(
            input: Value<*>, variables: CoercedVariables, graphQLContext: GraphQLContext, locale: Locale
        ): Long =
            when (input) {
                is IntValue -> input.value.longValueExact()
                is StringValue -> input.value.toLongOrNull()
                    ?: throw CoercingParseLiteralException("Cannot coerce '${input.value}' to Long")
                else -> throw CoercingParseLiteralException("Expected IntValue or StringValue but was '${input.javaClass}'")
            }
    })
    .build()

@Factory
class GraphQLFactory(
    @Inject private val queryFetchers: GraphQLQueryFetchers,
    @Inject private val mutationFetchers: GraphQLMutationFetchers,
    @Inject private val resourceResolver: ResourceResolver,
) {

    @Bean
    @Singleton
    fun graphQL(): GraphQL {
        val schemaStream = resourceResolver.getResourceAsStream("classpath:schema.graphqls")
            .orElseThrow { IllegalStateException("GraphQL schema not found at classpath:schema.graphqls") }

        val typeDefinitionRegistry = SchemaParser().parse(schemaStream.reader())

        val runtimeWiring = RuntimeWiring.newRuntimeWiring()
            .scalar(ExtendedScalars.GraphQLBigDecimal)
            .scalar(ExtendedScalars.Date)
            .scalar(timestampDateTimeScalar)
            .scalar(ExtendedScalars.GraphQLLong)
            .scalar(idScalar)
            .type(TypeRuntimeWiring.newTypeWiring("Query")
                .dataFetcher("accounts", queryFetchers.accounts())
                .dataFetcher("account", queryFetchers.account())
                .dataFetcher("transactions", queryFetchers.transactions())
                .dataFetcher("transaction", queryFetchers.transaction())
                .dataFetcher("categories", queryFetchers.categories())
                .dataFetcher("category", queryFetchers.category())
                .dataFetcher("descriptions", queryFetchers.descriptions())
                .dataFetcher("description", queryFetchers.description())
                .dataFetcher("payments", queryFetchers.payments())
                .dataFetcher("payment", queryFetchers.payment())
                .dataFetcher("transfers", queryFetchers.transfers())
                .dataFetcher("transfer", queryFetchers.transfer())
                .dataFetcher("parameters", queryFetchers.parameters())
                .dataFetcher("validationAmounts", queryFetchers.validationAmounts())
                .dataFetcher("receiptImages", queryFetchers.receiptImages())
                .dataFetcher("medicalExpenses", queryFetchers.medicalExpenses())
                .dataFetcher("medicalExpense", queryFetchers.medicalExpense())
                .dataFetcher("medicalExpensesByClaimStatus", queryFetchers.medicalExpensesByClaimStatus())
            )
            .type(TypeRuntimeWiring.newTypeWiring("Mutation")
                .dataFetcher("createAccount", mutationFetchers.createAccount())
                .dataFetcher("updateAccount", mutationFetchers.updateAccount())
                .dataFetcher("deleteAccount", mutationFetchers.deleteAccount())
                .dataFetcher("createTransaction", mutationFetchers.createTransaction())
                .dataFetcher("updateTransaction", mutationFetchers.updateTransaction())
                .dataFetcher("deleteTransaction", mutationFetchers.deleteTransaction())
                .dataFetcher("createCategory", mutationFetchers.createCategory())
                .dataFetcher("updateCategory", mutationFetchers.updateCategory())
                .dataFetcher("deleteCategory", mutationFetchers.deleteCategory())
                .dataFetcher("createDescription", mutationFetchers.createDescription())
                .dataFetcher("updateDescription", mutationFetchers.updateDescription())
                .dataFetcher("deleteDescription", mutationFetchers.deleteDescription())
                .dataFetcher("createPayment", mutationFetchers.createPayment())
                .dataFetcher("updatePayment", mutationFetchers.updatePayment())
                .dataFetcher("deletePayment", mutationFetchers.deletePayment())
                .dataFetcher("createTransfer", mutationFetchers.createTransfer())
                .dataFetcher("updateTransfer", mutationFetchers.updateTransfer())
                .dataFetcher("deleteTransfer", mutationFetchers.deleteTransfer())
                .dataFetcher("createParameter", mutationFetchers.createParameter())
                .dataFetcher("updateParameter", mutationFetchers.updateParameter())
                .dataFetcher("deleteParameter", mutationFetchers.deleteParameter())
                .dataFetcher("createValidationAmount", mutationFetchers.createValidationAmount())
                .dataFetcher("updateValidationAmount", mutationFetchers.updateValidationAmount())
                .dataFetcher("deleteValidationAmount", mutationFetchers.deleteValidationAmount())
                .dataFetcher("createMedicalExpense", mutationFetchers.createMedicalExpense())
                .dataFetcher("updateMedicalExpense", mutationFetchers.updateMedicalExpense())
                .dataFetcher("deleteMedicalExpense", mutationFetchers.deleteMedicalExpense())
            )
            .build()

        val executableSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        val schema = graphql.schema.GraphQLSchema.newSchema(executableSchema)
            .additionalType(idScalar)
            .build()
        return GraphQL.newGraphQL(schema).build()
    }
}
