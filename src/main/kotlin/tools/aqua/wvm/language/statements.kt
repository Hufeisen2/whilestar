/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2024-2024 The While* Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.wvm.language

import java.math.BigInteger
import java.util.Scanner
import kotlin.random.Random
import tools.aqua.wvm.analysis.semantics.*
import tools.aqua.wvm.machine.Configuration

interface PrintWithIndentation {
  fun toIndentedString(indent: String): String
}

sealed interface Statement : PrintWithIndentation {
  fun execute(cfg: Configuration, input: Scanner?): StatementApp
}

abstract class Assignment<T : Expression<*>>(val addr: AddressExpression, val expr: T) : Statement {

  abstract override fun execute(cfg: Configuration, input: Scanner?): StatementApp

  override fun equals(other: Any?) =
      (other is Assignment<*>) && other.addr == addr && other.expr == expr

  override fun hashCode() = addr.hashCode() + expr.hashCode()

  override fun toIndentedString(indent: String) = "${indent}$addr := $expr;\n"
}

class IntAssignment(addr: AddressExpression, expr: ArithmeticExpression) :
    Assignment<ArithmeticExpression>(addr, expr) {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val a = addr.evaluate(cfg.scope, cfg.memory)
    val e = expr.evaluate(cfg.scope, cfg.memory)

    if (a is Error)
        return NestedStatementError(
            "AssErr",
            a,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    if (e is Error)
        return NestedStatementError(
            "AssErr",
            e,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    return AssOk(
        a as AddressOk,
        e as ArithmeticExpressionOk,
        this,
        Transition(
            cfg,
            dst =
                Configuration(
                    SequenceOfStatements(cfg.statements.tail()),
                    cfg.scope,
                    cfg.memory.write(a.result, e.result))))
  }
}

class BooleanAssignment(addr: AddressExpression, expr: BooleanExpression) :
    Assignment<BooleanExpression>(addr, expr) {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val a = addr.evaluate(cfg.scope, cfg.memory)
    val e = expr.evaluate(cfg.scope, cfg.memory)

    if (a is Error)
        return NestedStatementError(
            "AssErr",
            a,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    if (e is Error)
        return NestedStatementError(
            "AssErr",
            e,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    return AssOk2(
        a as AddressOk,
        e as BooleanExpressionOk,
        this,
        Transition(
            cfg,
            dst =
                Configuration(
                    SequenceOfStatements(cfg.statements.tail()),
                    cfg.scope,
                    cfg.memory.write(a.result, e.result))))
  }
}

data class Swap(val left: AddressExpression, val right: AddressExpression) : Statement {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val a1 = left.evaluate(cfg.scope, cfg.memory)
    val a2 = right.evaluate(cfg.scope, cfg.memory)

    if (a1 is Error)
        return NestedStatementError(
            "SwapErr",
            a1,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    if (a2 is Error)
        return NestedStatementError(
            "SwapErr",
            a2,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    val e1 = cfg.memory.read(a1.result)
    val e2 = cfg.memory.read(a2.result)

    return SwapOk(
        a1 as AddressOk,
        a2 as AddressOk,
        this,
        Transition(
            cfg,
            dst =
                Configuration(
                    SequenceOfStatements(cfg.statements.tail()),
                    cfg.scope,
                    cfg.memory.write(a1.result, e2).write(a2.result, e1))))
  }

  override fun toIndentedString(indent: String) = "${indent}swap $left and $right;\n"
}

data class IfThenElse(
    val cond: BooleanExpression,
    val thenBlock: SequenceOfStatements,
    val elseBlock: SequenceOfStatements
) : Statement {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val b = cond.evaluate(cfg.scope, cfg.memory)
    if (b is Error)
        NestedStatementError(
            "IfErr",
            b,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    return if (b.result)
        IfTrue(
            b as BooleanExpressionOk,
            this,
            Transition(
                cfg,
                dst =
                    Configuration(
                        concat(thenBlock.statements, cfg.statements.tail()),
                        cfg.scope,
                        cfg.memory)))
    else
        IfFalse(
            b as BooleanExpressionOk,
            this,
            Transition(
                cfg,
                dst =
                    Configuration(
                        concat(elseBlock.statements, cfg.statements.tail()),
                        cfg.scope,
                        cfg.memory)))
  }

  override fun toIndentedString(indent: String) =
      "${indent}if ($cond) {\n" +
          thenBlock.toIndentedString("  $indent") +
          "${indent}} else {" +
          elseBlock.toIndentedString("  $indent") +
          "${indent}};\n"
}

data class While(
    val head: BooleanExpression,
    val body: SequenceOfStatements,
    val invariant: BooleanExpression = True
) : Statement {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val cond = head.evaluate(cfg.scope, cfg.memory)
    val invar = invariant.evaluate(cfg.scope, cfg.memory)

    if (cond is Error)
        NestedStatementError(
            "IfErr",
            cond,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    if (invar is Error)
        NestedStatementError(
            "IfErr",
            invar,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    if (!invar.result)
        return WhInvar(
            cond as BooleanExpressionOk,
            invar as BooleanExpressionOk,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    return if (cond.result)
        WhTrue(
            cond as BooleanExpressionOk,
            invar as BooleanExpressionOk,
            this,
            Transition(
                cfg,
                dst =
                    Configuration(
                        concat(body.statements, cfg.statements.statements), cfg.scope, cfg.memory)))
    else
        WhFalse(
            cond as BooleanExpressionOk,
            invar as BooleanExpressionOk,
            this,
            Transition(
                cfg,
                dst =
                    Configuration(
                        SequenceOfStatements(cfg.statements.tail()), cfg.scope, cfg.memory)))
  }

  override fun toIndentedString(indent: String) =
      "${indent}while ($head) invariant ($invariant) {\n" +
          body.toIndentedString("  $indent") +
          "${indent}};\n"
}

data class Print(val message: String, val values: List<ArithmeticExpression>) : Statement {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val eval = values.map { it.evaluate(cfg.scope, cfg.memory) as ArithmeticExpressionApp }
    if (eval.any { it is Error })
        return NestedStatementError(
            "PrintErr",
            eval.first { it is Error } as Error,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    var out = message
    if (values.isNotEmpty()) {
      out += values.map { it.evaluate(cfg.scope, cfg.memory).result }.joinToString(", ", " [", "]")
    }
    return PrintOk(
        eval.map { it as ArithmeticExpressionOk },
        this,
        Transition(
            cfg,
            output = out,
            dst =
                Configuration(SequenceOfStatements(cfg.statements.tail()), cfg.scope, cfg.memory)))
  }

  override fun toIndentedString(indent: String) =
      "print \"$message\"${values.joinToString(", ", ", ")};\n"
}

data class Havoc(
    val addr: AddressExpression,
    val lower: BigInteger = BigInteger.valueOf(-100L),
    val upper: BigInteger = BigInteger.valueOf(100)
) : Statement {
  init {
    if (lower >= upper) {
      throw IllegalArgumentException(
          "Bounds for external variables have to be ordered. Here $lower > ${upper.minus(BigInteger.ONE)}.")
    }
  }

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp {
    val a = addr.evaluate(cfg.scope, cfg.memory)
    if (a is Error)
        NestedStatementError(
            "HavocErr",
            a,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

    val number =
        input?.nextBigInteger() ?: Random.nextLong(lower.toLong(), upper.toLong()).toBigInteger()
    return if (number < lower || number > upper)
        HavocRangeErr(
            a as AddressOk,
            this,
            Transition(
                cfg, dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))
    else
        HavocOk(
            a as AddressOk,
            this,
            Transition(
                cfg,
                input = number,
                dst =
                    Configuration(
                        SequenceOfStatements(cfg.statements.tail()),
                        cfg.scope,
                        cfg.memory.write(a.result, number))))
  }

  override fun toIndentedString(indent: String) =
      "${indent}extern $addr $lower .. ${upper.minus(BigInteger.ONE)};\n"
}

data class Fail(val message: String) : Statement {

  override fun execute(cfg: Configuration, input: Scanner?): StatementApp =
      FailOk(
          this,
          Transition(
              cfg,
              output = "Fail with message: $message",
              dst = Configuration(SequenceOfStatements(), cfg.scope, cfg.memory, true)))

  override fun toIndentedString(indent: String) = "fail \"$message\"\n"
}

data class SequenceOfStatements(val statements: List<Statement> = emptyList()) :
    PrintWithIndentation {

  fun head() = statements.first()

  fun end() = statements.last()

  fun tail() = statements.slice(1 ..< statements.size)

  fun front() = statements.slice(0 ..< statements.size - 1)

  fun isExhausted() = statements.isEmpty()

  override fun toIndentedString(indent: String): String =
      statements.joinToString("", transform = { s -> s.toIndentedString(indent) })
}

fun concat(seq1: List<Statement>, seq2: List<Statement>) = SequenceOfStatements(seq1 + seq2)
