package com.vanbank.core.numbers

import com.vanbank.core.model.AccountType
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountNumberGeneratorTest {
    @Test
    fun `checking accounts start with 1`() {
        val number = AccountNumberGenerator.generate(AccountType.CHECKING, Random(1))
        assertEquals('1', number.first())
    }

    @Test
    fun `savings accounts start with 2`() {
        val number = AccountNumberGenerator.generate(AccountType.SAVINGS, Random(1))
        assertEquals('2', number.first())
    }

    @Test
    fun `numbers are 10 digits`() {
        val number = AccountNumberGenerator.generate(AccountType.CHECKING, Random(1))
        assertEquals(10, number.length)
    }

    @Test
    fun `accountTypeOf round-trips with generate`() {
        val checking = AccountNumberGenerator.generate(AccountType.CHECKING, Random(2))
        val savings = AccountNumberGenerator.generate(AccountType.SAVINGS, Random(2))
        assertEquals(AccountType.CHECKING, AccountNumberGenerator.accountTypeOf(checking))
        assertEquals(AccountType.SAVINGS, AccountNumberGenerator.accountTypeOf(savings))
    }
}
