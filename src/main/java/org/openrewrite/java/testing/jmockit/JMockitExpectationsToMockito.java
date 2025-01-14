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
package org.openrewrite.java.testing.jmockit;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class JMockitExpectationsToMockito extends Recipe {
    @Override
    public String getDisplayName() {
        return "Rewrite JMockit Expectations";
    }

    @Override
    public String getDescription() {
        return "Rewrites JMockit `Expectations` blocks to Mockito statements.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("mockit.Expectations", false),
                new RewriteExpectationsVisitor());
    }

    private static class RewriteExpectationsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDeclaration, ExecutionContext ctx) {
            J.MethodDeclaration md = super.visitMethodDeclaration(methodDeclaration, ctx);
            if (md.getBody() == null) {
                return md;
            }
            // rewrite the statements that are not mock expectations or verifications
            SetupStatementsRewriter ssr = new SetupStatementsRewriter(this, md.getBody());
            J.Block methodBody = ssr.rewriteMethodBody();
            List<Statement> statements = methodBody.getStatements();

            int bodyStatementIndex = 0;
            // iterate over each statement in the method body, find Expectations blocks and rewrite them
            while (bodyStatementIndex < statements.size()) {
                if (!JMockitUtils.isValidExpectationsNewClassStatement(statements.get(bodyStatementIndex))) {
                    bodyStatementIndex++;
                    continue;
                }
                ExpectationsBlockRewriter ebr = new ExpectationsBlockRewriter(this, ctx, methodBody,
                        ((J.NewClass) statements.get(bodyStatementIndex)), bodyStatementIndex);
                methodBody = ebr.rewriteMethodBody();
                // reset the iteration to the beginning of the method body since the number of statements has likely changed
                bodyStatementIndex = 0;
                statements = methodBody.getStatements();
            }
            return md.withBody(methodBody);
        }
    }
}
