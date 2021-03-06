/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package net.hydromatic.optiq;

import net.hydromatic.linq4j.expressions.Expression;

import java.util.*;

/**
 * A namespace for tables and table functions.
 *
 * <p>A schema can also contain sub-schemas, to any level of nesting. Most
 * providers have a limited number of levels; for example, most JDBC databases
 * have either one level ("schemas") or two levels ("database" and
 * "catalog").</p>
 *
 * <p>There may be multiple overloaded table functions with the same name but
 * different numbers or types of parameters.
 * For this reason, {@link #getTableFunctions} returns a list of all
 * members with the same name. Optiq will call
 * {@link Schemas#resolve(org.eigenbase.reltype.RelDataTypeFactory, String, java.util.Collection, java.util.List)}
 * to choose the appropriate one.</p>
 *
 * <p>The most common and important type of member is the one with no
 * arguments and a result type that is a collection of records. This is called a
 * <dfn>relation</dfn>. It is equivalent to a table in a relational
 * database.</p>
 *
 * <p>For example, the query</p>
 *
 * <blockquote>select * from sales.emps</blockquote>
 *
 * <p>is valid if "sales" is a registered
 * schema and "emps" is a member with zero parameters and a result type
 * of <code>Collection(Record(int: "empno", String: "name"))</code>.</p>
 *
 * <p>A schema may be nested within another schema; see
 * {@link Schema#getSubSchema(String)}.</p>
 */
public interface Schema {
  /**
   * Returns the parent schema, or null if this schema has no parent.
   */
  SchemaPlus getParentSchema();

  /**
   * Returns the name of this schema.
   *
   * <p>The name must not be null, and must be unique within its parent.
   * The root schema is typically named "".
   */
  String getName();

  /**
   * Returns a table with a given name, or null if not found.
   *
   * @param name Table name
   * @return Table, or null
   */
  Table getTable(String name);

  /**
   * Returns the names of the tables in this schema.
   */
  Set<String> getTableNames();

  /**
   * Returns a list of table functions in this schema with the given name, or
   * an empty list if there is no such table function.
   *
   * @param name Name of table function
   * @return List of table functions with given name, or empty list
   */
  Collection<TableFunction> getTableFunctions(String name);

  /**
   * Returns the names of the table functions in this schema.
   */
  Set<String> getTableFunctionNames();

  /**
   * Returns a sub-schema with a given name, or null.
   */
  Schema getSubSchema(String name);

  /**
   * Returns the names of this schema's child schemas.
   */
  Set<String> getSubSchemaNames();

  /**
   * Returns the expression by which this schema can be referenced in generated
   * code.
   */
  Expression getExpression();

  /** Returns whether the user is allowed to create new tables, table functions
   * and sub-schemas in this schema, in addition to those returned automatically
   * by methods such as {@link #getTable(String)}.
   *
   * <p>Even if this method returns true, the maps are not modified. Optiq
   * stores the defined objects in a wrapper object. */
  boolean isMutable();

  /** Table type. */
  enum TableType {
    /** A regular table. */
    TABLE,

    /** A relation whose contents are calculated by evaluating a SQL
     * expression. */
    VIEW,

    /** A table maintained by the system. Data dictionary tables, such as the
     * "TABLES" and "COLUMNS" table in the "metamodel" schema, examples of
     * system tables. */
    SYSTEM_TABLE,

    /** A table that is only visible to one connection. */
    LOCAL_TEMPORARY,

    /** A structure, similar to a view, that is the basis for auto-generated
     * materializations. It is either a single table or a collection of tables
     * that are joined via many-to-one relationships from a central hub table.
     * It is not available for queries, but is just used as an intermediate
     * structure during query planning. */
    STAR,

    /** Index table. (Used by Apache Phoenix.) */
    INDEX,

    /** Join table. (Used by Apache Phoenix.) */
    JOIN,
  }
}

// End Schema.java
