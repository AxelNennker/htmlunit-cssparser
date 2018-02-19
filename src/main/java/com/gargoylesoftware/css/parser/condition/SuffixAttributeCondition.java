/*
 * Copyright (c) 2018 Ronald Brill.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gargoylesoftware.css.parser.condition;

import java.io.Serializable;

import com.gargoylesoftware.css.parser.AbstractLocatable;

/**
 * @author Ronald Brill
 */
public class SuffixAttributeCondition extends AbstractLocatable implements Condition, Serializable {

    private final String localName_;
    private final String value_;

    public SuffixAttributeCondition(final String localName, final String value) {
        localName_ = localName;
        value_ = value;
    }

    @Override
    public ConditionType getConditionType() {
        return ConditionType.SUFFIX_ATTRIBUTE_CONDITION; //for now
    }

    public String getLocalName() {
        return localName_;
    }

    public String getValue() {
        return value_;
    }

    @Override
    public String toString() {
        final String value = getValue();
        if (value != null) {
            return "[" + getLocalName() + "$=\"" + value + "\"]";
        }
        return "[" + getLocalName() + "]";
    }
}