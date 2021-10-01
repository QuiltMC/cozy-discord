package org.quiltmc.community.database

@Suppress("VariableNaming", "PropertyName")
interface Entity<ID> {
    val _id: ID
}
