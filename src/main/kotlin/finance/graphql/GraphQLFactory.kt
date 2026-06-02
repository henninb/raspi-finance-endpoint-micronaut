package finance.graphql

import graphql.GraphQL
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeRuntimeWiring
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.core.io.ResourceResolver
import jakarta.inject.Inject
import jakarta.inject.Singleton

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
            .scalar(ExtendedScalars.DateTime)
            .scalar(ExtendedScalars.GraphQLLong)
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

        val schema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        return GraphQL.newGraphQL(schema).build()
    }
}
