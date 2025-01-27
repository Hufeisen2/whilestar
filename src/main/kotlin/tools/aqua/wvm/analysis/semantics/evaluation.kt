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

package tools.aqua.wvm.analysis.semantics

import java.math.BigInteger
import tools.aqua.wvm.language.*

sealed class Application<T>(val result: T)

sealed interface Error {
  fun getError(): String
}

// --------------------------------------------------------------------

sealed class AddressApp(result: Int) : Application<Int>(result)

sealed class AddressOk(result: Int) : AddressApp(result)

sealed class AddressError(addr: Int, private val error: String) : AddressApp(addr), Error {
  override fun getError() = error
}

class NestedAddressError(val ruleName: String, val nested: Error, val expr: AddressExpression) :
    AddressError(-1, "$ruleName: $expr.")

class VarOk(val v: Variable, addr: Int) : AddressOk(addr)

class VarErr(val v: Variable) : AddressError(-1, "Variable ${v.name} undefined.")

class DeRefOk(val refOk: AddressOk, val deRef: DeRef, readValue: Int) : AddressOk(readValue)

class DeRefAddressError(val refOk: AddressOk, val deRef: DeRef, refValue: Int) :
    AddressError(refValue, "Invalid address $refValue.")

class ArrayAccessOk(
    val addrOk: ValAtAddrOk,
    val indexOk: ArithmeticExpressionOk,
    val arrayAccess: ArrayAccess,
    addrValue: Int
) : AddressOk(addrValue)

class ArrayAccessError(
    val arrayOk: ValAtAddrOk,
    val indexOk: ArithmeticExpressionOk,
    val arrayAccess: ArrayAccess,
    address: Int
) : AddressError(address, "Invalid address $address.")

// --------------------------------------------------------------------

sealed class ArithmeticExpressionApp(result: BigInteger) : Application<BigInteger>(result)

sealed class ArithmeticExpressionOk(result: BigInteger) : ArithmeticExpressionApp(result)

sealed class ArithmeticExpressionError(private val error: String) :
    ArithmeticExpressionApp(BigInteger.ZERO), Error {
  override fun getError() = error
}

class NestedArithmeticError(
    val ruleName: String,
    val nested: Error,
    val expr: ArithmeticExpression
) : ArithmeticExpressionError("$ruleName: $expr.")

class AddOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val add: Add,
    sum: BigInteger
) : ArithmeticExpressionOk(sum)

class SubOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val sub: Sub,
    diff: BigInteger
) : ArithmeticExpressionOk(diff)

class MulOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val mul: Mul,
    product: BigInteger
) : ArithmeticExpressionOk(product)

class DivOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val div: Div,
    quotient: BigInteger
) : ArithmeticExpressionOk(quotient)

class DivZeroErr(val rightOk: ArithmeticExpressionOk, val div: Div) :
    ArithmeticExpressionError("Division By Zero in $div")

class RemOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val rem: Rem,
    remainder: BigInteger
) : ArithmeticExpressionOk(remainder)

class RemZeroErr(val rightOk: ArithmeticExpressionOk, val rem: Rem) :
    ArithmeticExpressionError("Division By Zero in $rem")

class UnaryMinusOk(
    val negated: ArithmeticExpressionOk,
    val unaryMinus: UnaryMinus,
    result: BigInteger
) : ArithmeticExpressionOk(result)

class ValAtAddrOk(val addrOk: AddressOk, val valAtAddr: ValAtAddr, value: BigInteger) :
    ArithmeticExpressionOk(value)

class VarAddrOk(val addrOk: AddressOk, val varAddress: VarAddress, value: BigInteger) :
    ArithmeticExpressionOk(value)

class NumericLiteralOk(val n: NumericLiteral) : ArithmeticExpressionOk(n.literal)

// --------------------------------------------------------------------

sealed class BooleanExpressionApp(result: Boolean) : Application<Boolean>(result)

sealed class BooleanExpressionOk(result: Boolean) : BooleanExpressionApp(result)

sealed class BooleanExpressionError(private val error: String) :
    BooleanExpressionApp(false), Error {
  override fun getError() = error
}

class NestedBooleanError(val ruleName: String, val nested: Error, val expr: Expression<*>) :
    BooleanExpressionError("$ruleName: $expr.")

class BoolOk(val exprOk: ArithmeticExpressionOk, val gt: Bool, result: Boolean) :
    BooleanExpressionOk(result)

class EqOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val eq: Eq,
    result: Boolean
) : BooleanExpressionOk(result)

class GtOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val gt: Gt,
    result: Boolean
) : BooleanExpressionOk(result)

class GteOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val gte: Gte,
    result: Boolean
) : BooleanExpressionOk(result)

class LtOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val lt: Lt,
    result: Boolean
) : BooleanExpressionOk(result)

class LteOk(
    val leftOk: ArithmeticExpressionOk,
    val rightOk: ArithmeticExpressionOk,
    val lte: Lte,
    result: Boolean
) : BooleanExpressionOk(result)

class AndOk(
    val leftOk: BooleanExpressionOk,
    val rightOk: BooleanExpressionOk,
    val and: And,
    result: Boolean
) : BooleanExpressionOk(result)

class OrOk(
    val leftOk: BooleanExpressionOk,
    val rightOk: BooleanExpressionOk,
    val or: Or,
    result: Boolean
) : BooleanExpressionOk(result)

class ImplyOk(
    val leftOk: BooleanExpressionOk,
    val rightOk: BooleanExpressionOk,
    val imply: Imply,
    result: Boolean
) : BooleanExpressionOk(result)

class EquivOk(
    val leftOk: BooleanExpressionOk,
    val rightOk: BooleanExpressionOk,
    val equiv: Equiv,
    result: Boolean
) : BooleanExpressionOk(result)

class NotOk(val negated: BooleanExpressionOk, val not: Not, result: Boolean) :
    BooleanExpressionOk(result)

class TrueOk(val tru: True) : BooleanExpressionOk(true)

class FalseOk(val fls: False) : BooleanExpressionOk(false)

// --------------------------------------------------------------------

sealed class StatementApp(result: Transition) : Application<Transition>(result)

sealed class StatementOk(result: Transition) : StatementApp(result)

sealed class StatementError(private val error: String, result: Transition) :
    StatementApp(result), Error {
  override fun getError() = error
}

class NestedStatementError(
    val ruleName: String,
    val nested: Error,
    val stmt: Statement,
    result: Transition
) : StatementError("$ruleName: $stmt.", result)

class AssOk(
    val addr: AddressOk,
    val expr: ArithmeticExpressionOk,
    val assign: IntAssignment,
    trans: Transition
) : StatementOk(trans)

class AssOk2(
    val addr: AddressOk,
    val expr: BooleanExpressionOk,
    val assign: BooleanAssignment,
    trans: Transition
) : StatementOk(trans)

class SwapOk(val a1: AddressOk, val a2: AddressOk, val swap: Swap, trans: Transition) :
    StatementOk(trans)

class IfTrue(val b: BooleanExpressionOk, val ifThenElse: IfThenElse, trans: Transition) :
    StatementOk(trans)

class IfFalse(val b: BooleanExpressionOk, val ifThenElse: IfThenElse, trans: Transition) :
    StatementOk(trans)

class WhInvar(
    val cond: BooleanExpressionOk,
    val invar: BooleanExpressionOk,
    val wh: While,
    trans: Transition
) : StatementOk(trans)

class WhTrue(
    val cond: BooleanExpressionOk,
    val invar: BooleanExpressionOk,
    val wh: While,
    trans: Transition
) : StatementOk(trans)

class WhFalse(
    val cond: BooleanExpressionOk,
    val invar: BooleanExpressionOk,
    val wh: While,
    trans: Transition
) : StatementOk(trans)

class PrintOk(val expr: List<ArithmeticExpressionOk>, val print: Print, trans: Transition) :
    StatementOk(trans)

class HavocOk(val addr: AddressOk, val havoc: Havoc, trans: Transition) : StatementOk(trans)

class HavocRangeErr(val addr: AddressOk, val havoc: Havoc, trans: Transition) : StatementOk(trans)

class FailOk(val fail: Fail, trans: Transition) : StatementOk(trans)
