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

package org.partiql.pig.runtime

import com.amazon.ionelement.api.IonElectrolyteException
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class  IonElementTransformerBaseTests {

    abstract class DummyDomainNode : DomainNode

    data class CorrectDomainNode(val someString: String, override val metas: MetaContainer = emptyMetaContainer()): DummyDomainNode() {
        override fun withMeta(key: String, value: Any): DomainNode {
            error("does not need to be implemented for tests")
        }
        override fun toIonElement(): IonElement {
            error("does not need to be implemented for tests")
        }
    }

    data class IncorrectDomainNode(val someString: String): DummyDomainNode() {
        override fun withMeta(key: String, value: Any): DomainNode {
            error("does not need to be implemented for tests")
        }
        override val metas: MetaContainer
            get() = error("does not need to be implemented for tests")

        override fun toIonElement(): IonElement {
            error("does not need to be implemented for tests")
        }
    }

    class DummyIonElementTransformer : IonElementTransformerBase<DummyDomainNode>() {
        override fun innerTransform(maybeSexp: IonElement): DummyDomainNode {
            return CorrectDomainNode("foo")
        }

        fun expectedDomainNode(): DomainNode = ionString("doesntmatter").transformExpect<CorrectDomainNode>()

        fun unexpectedDomainNode() {
            // Throws MalformedDomainDataException because [innerTransform] returns an instance of
            // [IncorrectDomainType].
            ionString("doesntmatter").transformExpect<IncorrectDomainNode>()
        }
    }

    class DummyIonElementTransformerThrowing : IonElementTransformerBase<DummyDomainNode>() {
        override fun innerTransform(maybeSexp: IonElement): DummyDomainNode {
            throw IonElectrolyteException(null, "oh_my_an_error")
        }
    }

    @Test
    fun transformExpect_correctType() {
        val xformer = DummyIonElementTransformer()
        assertTrue(xformer.expectedDomainNode() is CorrectDomainNode)
    }

    @Test
    fun transformExpect_incorrectType() {
        val xformer = DummyIonElementTransformer()
        val ex = assertThrows<MalformedDomainDataException> { xformer.unexpectedDomainNode() }
        assertTrue(ex.message!!.contains("IncorrectDomainNode"))
    }

    @Test
    fun handlesIonElectrolyteException() {
        val xformer = DummyIonElementTransformerThrowing()
        val ex = assertThrows<MalformedDomainDataException> { xformer.transform(ionString("doesntmatter")) }

        assertTrue(ex.cause is IonElectrolyteException)
        assertTrue(ex.message!!.contains("oh_my_an_error"))
    }

}