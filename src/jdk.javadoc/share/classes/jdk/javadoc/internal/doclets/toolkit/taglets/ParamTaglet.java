/*
 * Copyright (c) 2001, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.javadoc.internal.doclets.toolkit.taglets;

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import jdk.javadoc.doclet.Taglet.Location;
import jdk.javadoc.internal.doclets.toolkit.Content;
import jdk.javadoc.internal.doclets.toolkit.Messages;
import jdk.javadoc.internal.doclets.toolkit.util.CommentHelper;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder;
import jdk.javadoc.internal.doclets.toolkit.util.DocFinder.Input;
import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * A taglet that represents the {@code @param} tag.
 */
public class ParamTaglet extends BaseTaglet implements InheritableTaglet {
    public enum ParamKind {
        /** Parameter of an executable element. */
        PARAMETER,
        /** State components of a record. */
        RECORD_COMPONENT,
        /** Type parameters of an executable element or type element. */
        TYPE_PARAMETER
    }

    /**
     * Construct a ParamTaglet.
     */
    public ParamTaglet() {
        super(DocTree.Kind.PARAM, false, EnumSet.of(Location.TYPE, Location.CONSTRUCTOR, Location.METHOD));
    }

    /**
     * Given a list of parameters, returns a name-position map.
     * @param params the list of parameters from a type or an executable member
     * @return a name-position map
     */
    private static Map<String, String> mapNameToPosition(Utils utils, List<? extends Element> params) {
        Map<String, String> result = new HashMap<>();
        int position = 0;
        for (Element e : params) {
            String name = utils.isTypeParameterElement(e)
                    ? utils.getTypeName(e.asType(), false)
                    : utils.getSimpleName(e);
            result.put(name, Integer.toString(position));
            position++;
        }
        return result;
    }

    @Override
    public void inherit(DocFinder.Input input, DocFinder.Output output) {
        Utils utils = input.utils;
        if (input.tagId == null) {
            input.isTypeVariableParamTag = ((ParamTree) input.docTreeInfo.docTree()).isTypeParameter();
            ExecutableElement ee = (ExecutableElement) input.docTreeInfo.element();
            CommentHelper ch = utils.getCommentHelper(ee);
            List<? extends Element> parameters = input.isTypeVariableParamTag
                    ? ee.getTypeParameters()
                    : ee.getParameters();
            String target = ch.getParameterName((ParamTree) input.docTreeInfo.docTree());
            for (int i = 0; i < parameters.size(); i++) {
                Element e = parameters.get(i);
                String pname = input.isTypeVariableParamTag
                        ? utils.getTypeName(e.asType(), false)
                        : utils.getSimpleName(e);
                if (pname.contentEquals(target)) {
                    input.tagId = Integer.toString(i);
                    break;
                }
            }
        }
        ExecutableElement md = (ExecutableElement) input.element;
        CommentHelper ch = utils.getCommentHelper(md);
        List<? extends ParamTree> tags = input.isTypeVariableParamTag
                ? utils.getTypeParamTrees(md)
                : utils.getParamTrees(md);
        List<? extends Element> parameters = input.isTypeVariableParamTag
                ? md.getTypeParameters()
                : md.getParameters();
        Map<String, String> positionOfName = mapNameToPosition(utils, parameters);
        for (ParamTree tag : tags) {
            String paramName = ch.getParameterName(tag);
            if (positionOfName.containsKey(paramName) && positionOfName.get(paramName).equals((input.tagId))) {
                output.holder = input.element;
                output.holderTag = tag;
                output.inlineTags = ch.getBody(tag);
                return;
            }
        }
    }

    @Override
    public Content getAllBlockTagOutput(Element holder, TagletWriter writer) {
        Utils utils = writer.configuration().utils;
        if (utils.isExecutableElement(holder)) {
            ExecutableElement member = (ExecutableElement) holder;
            Content output = getTagletOutput(ParamKind.TYPE_PARAMETER, member, writer,
                    member.getTypeParameters(), utils.getTypeParamTrees(member));
            output.add(getTagletOutput(ParamKind.PARAMETER, member, writer,
                    member.getParameters(), utils.getParamTrees(member)));
            return output;
        } else {
            TypeElement typeElement = (TypeElement) holder;
            Content output = getTagletOutput(ParamKind.TYPE_PARAMETER, typeElement, writer,
                    typeElement.getTypeParameters(), utils.getTypeParamTrees(typeElement));
            output.add(getTagletOutput(ParamKind.RECORD_COMPONENT, typeElement, writer,
                    typeElement.getRecordComponents(), utils.getParamTrees(typeElement)));
            return output;
        }
    }

    /**
     * Given an array of {@code @param DocTree}s, return its string representation.
     * Try to inherit the param tags that are missing.
     *
     * @param holder            the element that holds the param tags.
     * @param writer            the TagletWriter that will write this tag.
     * @param formalParameters  The array of parameters (from type or executable
     *                          member) to check.
     *
     * @return the content representation of these {@code @param DocTree}s.
     */
    private Content getTagletOutput(ParamKind kind,
                                    Element holder,
                                    TagletWriter writer,
                                    List<? extends Element> formalParameters,
                                    List<? extends ParamTree> paramTags) {
        Content result = writer.getOutputInstance();
        result.add(processParamTags(holder, kind, paramTags, formalParameters, writer));
        return result;
    }

    /**
     * Try to get the inherited taglet documentation for a specific parameter.
     */
    private Content getInheritedTagletOutput(ParamKind kind,
                                             Element holder,
                                             TagletWriter writer,
                                             Element param,
                                             int position,
                                             boolean isFirst) {
        Utils utils = writer.configuration().utils;
        Content result = writer.getOutputInstance();
        Input input = new DocFinder.Input(writer.configuration().utils, holder, this,
                Integer.toString(position), kind == ParamKind.TYPE_PARAMETER);
        DocFinder.Output inheritedDoc = DocFinder.search(writer.configuration(), input);
        if (!inheritedDoc.inlineTags.isEmpty()) {
            String lname = kind != ParamKind.TYPE_PARAMETER
                    ? utils.getSimpleName(param)
                    : utils.getTypeName(param.asType(), false);
            Content content = processParamTag(inheritedDoc.holder, kind, writer,
                    (ParamTree) inheritedDoc.holderTag,
                    lname, isFirst);
            result.add(content);
        }
        return result;
    }

    /**
     * Returns a {@code Content} representation of a list of {@code ParamTree}.
     *
     * <p> This method:
     * <ul>
     *   <li> correlates ParamTree with Element by name
     *   <li> warns about {@code @param} tags that do not map to parameter
     *        elements and param tags that are duplicated
     * </ul>
     * </p>
     *
     * @param e the element
     * @param kind the kind of all parameters in the lists
     * @param paramTags the list of {@code ParamTree}
     * @param formalParameters the list of parameter elements
     * @param writer the TagletWriter that will write this tag
     * @return the {@code Content} representation
     */
    private Content processParamTags(Element e,
                                     ParamKind kind,
                                     List<? extends ParamTree> paramTags,
                                     List<? extends Element> formalParameters,
                                     TagletWriter writer) {
        Map<String, ParamTree> tagOfPosition = new HashMap<>();
        Messages messages = writer.configuration().getMessages();
        CommentHelper ch = writer.configuration().utils.getCommentHelper(e);
        if (!paramTags.isEmpty()) {
            Map<String, String> positionOfName = mapNameToPosition(writer.configuration().utils, formalParameters);
            for (ParamTree dt : paramTags) {
                String name = ch.getParameterName(dt);
                String paramName = kind == ParamKind.TYPE_PARAMETER ? "<" + name + ">" : name;
                if (!positionOfName.containsKey(name)) {
                    String key = switch (kind) {
                        case PARAMETER -> "doclet.Parameters_warn";
                        case TYPE_PARAMETER -> "doclet.TypeParameters_warn";
                        case RECORD_COMPONENT -> "doclet.RecordComponents_warn";
                    };
                    messages.warning(ch.getDocTreePath(dt), key, paramName);
                }
                String position = positionOfName.get(name);
                if (position != null) {
                    if (tagOfPosition.containsKey(position)) {
                        String key = switch (kind) {
                            case PARAMETER -> "doclet.Parameters_dup_warn";
                            case TYPE_PARAMETER -> "doclet.TypeParameters_dup_warn";
                            case RECORD_COMPONENT -> "doclet.RecordComponents_dup_warn";
                        };
                        messages.warning(ch.getDocTreePath(dt), key, paramName);
                    } else {
                        tagOfPosition.put(position, dt);
                    }
                }
            }
        }
        // Document declared parameters for which taglet documentation is available
        // (either directly or inherited) in order of their declaration.
        Content result = writer.getOutputInstance();
        for (int i = 0; i < formalParameters.size(); i++) {
            ParamTree dt = tagOfPosition.get(Integer.toString(i));
            if (dt != null) {
                result.add(processParamTag(e, kind, writer, dt,
                        ch.getParameterName(dt), result.isEmpty()));
            } else if (writer.configuration().utils.isMethod(e)) {
                result.add(getInheritedTagletOutput(kind, e, writer,
                        formalParameters.get(i), i, result.isEmpty()));
            }
        }
        if (paramTags.size() > tagOfPosition.size()) {
            // Generate documentation for remaining taglets that do not match a declared parameter.
            // These are erroneous but we generate them anyway.
            for (ParamTree dt : paramTags) {
                if (!tagOfPosition.containsValue(dt)) {
                    result.add(processParamTag(e, kind, writer, dt,
                            ch.getParameterName(dt), result.isEmpty()));
                }
            }
        }
        return result;
    }

    /**
     * Convert the individual ParamTag into Content.
     *
     * @param e               the owner element
     * @param kind            the kind of param tag
     * @param writer          the taglet writer for output writing.
     * @param paramTag        the tag whose inline tags will be printed.
     * @param name            the name of the parameter.  We can't rely on
     *                        the name in the param tag because we might be
     *                        inheriting documentation.
     * @param isFirstParam    true if this is the first param tag being printed.
     *
     */
    private Content processParamTag(Element e,
                                    ParamKind kind,
                                    TagletWriter writer,
                                    ParamTree paramTag,
                                    String name,
                                    boolean isFirstParam) {
        Content result = writer.getOutputInstance();
        if (isFirstParam) {
            result.add(writer.getParamHeader(kind));
        }
        result.add(writer.paramTagOutput(e, paramTag, name));
        return result;
    }
}
