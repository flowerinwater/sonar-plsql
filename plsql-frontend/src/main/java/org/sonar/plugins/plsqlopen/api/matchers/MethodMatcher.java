/*
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2019 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.plsqlopen.api.matchers;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.sonar.plugins.plsqlopen.api.squid.SemanticAstNode;
import org.sonar.plugins.plsqlopen.api.PlSqlGrammar;
import org.sonar.plugins.plsqlopen.api.symbols.PlSqlType;

import com.google.common.base.Preconditions;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;

public class MethodMatcher {

    private static final AstNodeType[] VARIABLE_OR_IDENTIFIER = { PlSqlGrammar.VARIABLE_NAME, PlSqlGrammar.IDENTIFIER_NAME };
    
    private NameCriteria methodNameCriteria;
    private NameCriteria packageNameCriteria;
    private NameCriteria schemaNameCriteria;
    private boolean shouldCheckParameters = true;
    private boolean schemaIsOptional = false;
    private String methodName;
    private List<PlSqlType> expectedArgumentTypes = new ArrayList<>();

    private MethodMatcher() {
        // instances should be created using the create method
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public static MethodMatcher create() {
        return new MethodMatcher();
    }
    
    public MethodMatcher name(String methodNameCriteria) {
        return name(NameCriteria.is(methodNameCriteria));
    }
    
    public MethodMatcher name(NameCriteria methodNameCriteria) {
        Preconditions.checkState(this.methodNameCriteria == null);
        this.methodNameCriteria = methodNameCriteria;
        return this;
    }
    
    public MethodMatcher packageName(String packageNameCriteria) {
        return packageName(NameCriteria.is(packageNameCriteria));
    }
    
    public MethodMatcher packageName(NameCriteria packageNameCriteria) {
        Preconditions.checkState(this.packageNameCriteria == null);
        this.packageNameCriteria = packageNameCriteria;
        return this;
    }
    
    public MethodMatcher schema(String schemaNameCriteria) {
        return schema(NameCriteria.is(schemaNameCriteria));
    }
    
    public MethodMatcher schema(NameCriteria schemaNameCriteria) {
        Preconditions.checkState(this.schemaNameCriteria == null);
        this.schemaNameCriteria = schemaNameCriteria;
        return this;
    }
    
    public MethodMatcher withNoParameterConstraint() {
        Preconditions.checkState(this.expectedArgumentTypes.isEmpty());
        this.shouldCheckParameters = false;
        return this;
    }

    public MethodMatcher schemaIsOptional() {
        this.schemaIsOptional = true;
        return this;
    }
    
    public MethodMatcher addParameter() {
        addParameter(PlSqlType.UNKNOWN);
        return this;
    }

    public MethodMatcher addParameter(PlSqlType type) {
        Preconditions.checkState(this.shouldCheckParameters);
        expectedArgumentTypes.add(type);
        return this;
    }
    
    public MethodMatcher addParameters(int quantity) {
        for (int i = 0; i < quantity; i++) {
            addParameter(PlSqlType.UNKNOWN);
        }
        return this;
    }

    public MethodMatcher addParameters(PlSqlType... types) {
        Preconditions.checkState(this.shouldCheckParameters);
        for (PlSqlType type : types) {
            addParameter(type);
        }
        return this;
    }
    
    public List<AstNode> getArguments(AstNode node) {
        AstNode arguments = node.getFirstChild(PlSqlGrammar.ARGUMENTS);
        if (arguments != null) {
            return arguments.getChildren(PlSqlGrammar.ARGUMENT);
        }
        
        return new ArrayList<>();
    }
    
    public List<AstNode> getArgumentsValues(AstNode node) {
        return getArguments(node).stream().map(AstNode::getLastChild).collect(toList());
    }

    public boolean matches(AstNode originalNode) {
        AstNode node = normalize(originalNode);
        LinkedList<AstNode> nodes = new LinkedList<>(node.getChildren(VARIABLE_OR_IDENTIFIER));
        
        if (nodes.isEmpty()) {
            return false;
        }
        
        boolean matches = nameAcceptable(nodes.removeLast(), methodNameCriteria);
        
        if (packageNameCriteria != null) {
            matches &= !nodes.isEmpty() && nameAcceptable(nodes.removeLast(), packageNameCriteria);
        }
        
        if (schemaNameCriteria != null) {
            matches &= (schemaIsOptional && nodes.isEmpty()) || 
                    (!nodes.isEmpty() && nameAcceptable(nodes.removeLast(), schemaNameCriteria));
        }
        
        return matches && nodes.isEmpty() && argumentsAcceptable(originalNode);
    }

    private boolean nameAcceptable(AstNode node, NameCriteria criteria) {
        methodName = node.getTokenOriginalValue();
        return criteria.matches(methodName);
    }

    private boolean argumentsAcceptable(AstNode node) {
        List<AstNode> arguments = getArguments(node);
        return !shouldCheckParameters ||
            (arguments.size() == expectedArgumentTypes.size() && argumentTypesAreCorrect(arguments));
    }

    private boolean argumentTypesAreCorrect(List<AstNode> arguments) {
        boolean result = true;
        int i = 0;
        for (PlSqlType type : expectedArgumentTypes) {
            AstNode actualArgument = arguments.get(i++).getFirstChild();
            result &= (type == PlSqlType.UNKNOWN || type == semantic(actualArgument).getPlSqlType());
        }
        return result;
    }

    static SemanticAstNode semantic(AstNode node) {
        return (SemanticAstNode)node;
    }

    private static AstNode normalize(AstNode node) {
        if (node.getType() == PlSqlGrammar.METHOD_CALL || node.getType() == PlSqlGrammar.CALL_STATEMENT) {
            AstNode child = normalize(node.getFirstChild());
            if (child.getFirstChild().getType() == PlSqlGrammar.HOST_AND_INDICATOR_VARIABLE) {
                child = child.getFirstChild();
            }
            return child;
        }
        return node;
    }
    
}
