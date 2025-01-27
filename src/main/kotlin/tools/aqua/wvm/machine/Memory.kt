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

package tools.aqua.wvm.machine

import java.math.BigInteger

class Memory(private val store: Array<BigInteger>) {

  fun read(addr: Int): BigInteger {
    checkBounds(addr)
    return store[addr]
  }

  fun write(addr: Int, value: BigInteger): Memory {
    checkBounds(addr)
    val copy = store.copyOf()
    copy[addr] = value
    return Memory(copy)
  }

  fun write(addr: Int, value: Boolean): Memory {
    checkBounds(addr)
    val copy = store.copyOf()
    copy[addr] = if (value) BigInteger.ONE else BigInteger.ZERO
    return Memory(copy)
  }

  private fun checkBounds(addr: Int) {
    if (addr < 0 || store.size < addr) {
      throw Exception("segmentation fault: tried to access invalid address: $addr")
    }
  }

  fun size() = store.size

  override fun toString(): String =
      "Addresses values: ${store.mapIndexed{idx, v -> "${idx} -> ${v}"}}"
}
