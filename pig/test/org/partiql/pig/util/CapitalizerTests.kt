/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.partiql.pig.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CapitalizerTests {
    
    
    @Test
    fun snakeToPascalCase() {
        assertEquals("One", "one".snakeToPascalCase())
        assertEquals("One", "_one".snakeToPascalCase())
        assertEquals("One", "one_".snakeToPascalCase())
        assertEquals("One", "_one_".snakeToPascalCase())
        assertEquals("OneTwo", "one_two".snakeToPascalCase())
        assertEquals("OneTwo", "_one_two".snakeToPascalCase())
        assertEquals("OneTwo", "one_two_".snakeToPascalCase())
        assertEquals("OneTwo", "_one_two_".snakeToPascalCase())
        assertEquals("OneTwoThree", "one_two_three".snakeToPascalCase())
        assertEquals("OneTwo", "one__two".snakeToPascalCase())
    }
    @Test
    fun snakeToCamelCase() {
        assertEquals("one", "one".snakeToCamelCase())
        assertEquals("one", "_one".snakeToCamelCase())
        assertEquals("one", "one_".snakeToCamelCase())
        assertEquals("one", "_one_".snakeToCamelCase())
        assertEquals("oneTwo", "one_two".snakeToCamelCase())
        assertEquals("oneTwo", "_one_two".snakeToCamelCase())
        assertEquals("oneTwo", "one_two_".snakeToCamelCase())
        assertEquals("oneTwo", "_one_two_".snakeToCamelCase())
        assertEquals("oneTwoThree", "one_two_three".snakeToCamelCase())
        assertEquals("oneTwo", "one__two".snakeToCamelCase())
    }
}