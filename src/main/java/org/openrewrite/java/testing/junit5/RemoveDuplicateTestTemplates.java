/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.junit5;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

public class RemoveDuplicateTestTemplates extends Recipe {
    private static final AnnotationMatcher TEST_ANNOTATION_MATCHER = new AnnotationMatcher("@org.junit.jupiter.api.Test");
    private static final AnnotationMatcher REPEATED_TEST_ANNOTATION_MATCHER = new AnnotationMatcher("@org.junit.jupiter.api.RepeatedTest");

    public String getDisplayName() {
        return "Remove duplicates uses of @TestTemplate implementations for a single method";
    }

    public String getDescription() {
        return "Remove duplicates uses of @TestTemplate implementations for a single method.";
    }

    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("org.junit.jupiter.api.RepeatedTest", false),
                new RemoveDuplicateTestTemplateVisitor());
    }

    private static class RemoveDuplicateTestTemplateVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration md, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(md, ctx);
            // first check if @Test or @RepeatedTest is present, else return early
            if (m.getLeadingAnnotations().stream().anyMatch(TEST_ANNOTATION_MATCHER::matches) &&
                m.getLeadingAnnotations().stream().anyMatch(REPEATED_TEST_ANNOTATION_MATCHER::matches)) {
                maybeRemoveImport("org.junit.jupiter.api.Test");
                return new RemoveAnnotationVisitor(TEST_ANNOTATION_MATCHER).visitMethodDeclaration(m, ctx);
            }
            return m;
        }
    }
}
