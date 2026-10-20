/*
 * Copyright 2026 The sbomguard Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nephets.sbomguard.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 解析异常是 contract 层与 store 层之间唯一的错误传递通道，必须被充分覆盖。 */
class SbomParseExceptionTest {
    @Test
    void messageConstructorSetsMessage() {
        SbomParseException e = new SbomParseException("boom");
        assertThat(e).isInstanceOf(RuntimeException.class);
        assertThat(e.getMessage()).isEqualTo("boom");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void causeConstructorSetsCauseAndMessage() {
        Throwable cause = new IllegalStateException("root");
        SbomParseException e = new SbomParseException("wrap", cause);
        assertThat(e.getMessage()).isEqualTo("wrap");
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    void thrownAsRuntimeException() {
        assertThatThrownBy(
                        () -> {
                            throw new SbomParseException("x");
                        })
                .isInstanceOf(SbomParseException.class)
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void causeIsRetainedForInspection() {
        Throwable cause = new RuntimeException("c");
        SbomParseException e = new SbomParseException("m", cause);
        assertThat(e.getCause().getMessage()).isEqualTo("c");
    }

    @Test
    void nullMessageAllowed() {
        SbomParseException e = new SbomParseException(null);
        assertThat(e.getMessage()).isNull();
    }
}
