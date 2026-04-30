package com.klasio.programclass.domain.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClassLevelTest {

    @Test
    void exposesFourLevelsIncludingOpen() {
        assertThat(ClassLevel.values())
                .containsExactly(ClassLevel.BEGINNER, ClassLevel.INTERMEDIATE,
                        ClassLevel.ADVANCED, ClassLevel.OPEN);
    }
}
