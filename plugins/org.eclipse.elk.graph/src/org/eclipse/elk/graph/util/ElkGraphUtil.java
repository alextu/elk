/*******************************************************************************
 * Copyright (c) 2016 Kiel University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    cds - initial API and implementation
 *******************************************************************************/
package org.eclipse.elk.graph.util;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.eclipse.elk.graph.ElkConnectableShape;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkGraphElement;
import org.eclipse.elk.graph.ElkGraphFactory;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.ElkPort;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Utility methods that make using the ElkGraph data structure a bit easier and thereby make ELK great again! The
 * class offers different types of methods described below.
 * 
 * 
 * <h2>Factory Methods</h2>
 * 
 * <p>While {@link ElkGraphFactory} offers methods that simply create new model objects, the factory methods
 * offered by this class are designed to make creating a graph programmatically as easy as possible. This
 * includes automatically setting containments and may at some point also include applying sensible defaults,
 * where applicable. The latter is why using these methods instead of factory methods is usually a good
 * idea.</p>
 * 
 * 
 * <h2>Convenience Methods</h2>
 * 
 * <p>This class offers quite a few convenience methods, including easy iteration over end points of an edge
 * or edges incident to a node, a way to find the node a connectable shape represents, as well as ways to
 * interact with a graph's structure.</p>
 * 
 * TODO: More documentation about what's in here.
 */
public final class ElkGraphUtil {
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Creation
    
    /**
     * Creates a new root node that will represent a graph.
     * 
     * @return the root node.
     */
    public static ElkNode createGraph() {
        return createNode(null);
    }
    
    /**
     * Creates a new node in the graph represented by the given parent node.
     * 
     * @param parent the parent node. May be {@code null}, in which case the new node is not added to anything.
     * @return the new node.
     */
    public static ElkNode createNode(final ElkNode parent) {
        ElkNode node = ElkGraphFactory.eINSTANCE.createElkNode();
        
        if (parent != null) {
            node.setParent(parent);
        }
        
        return node;
    }

    /**
     * Creates a new port for the given parent node.
     * 
     * @param parent the parent node. May be {@code null}, in which case the new port is not added to anything.
     * @return the new port.
     */
    public static ElkPort createPort(final ElkNode parent) {
        ElkPort port = ElkGraphFactory.eINSTANCE.createElkPort();
        
        if (parent != null) {
            port.setParent(parent);
        }
        
        return port;
    }

    /**
     * Creates a new label for the given graph element.
     * 
     * @param parent the parent element. May be {@code null}, in which case the new label is not added to anything.
     * @return the new label.
     */
    public static ElkLabel createLabel(final ElkGraphElement parent) {
        ElkLabel label = ElkGraphFactory.eINSTANCE.createElkLabel();
        
        if (parent != null) {
            label.setParent(parent);
        }
        
        return label;
    }
    
    /**
     * Creates a new edge contained in the given node, but not connecting anything yet. Note that the containing
     * node defines the coordinate system for the edge's routes and is not straightforward to select. One way to get
     * around this issue is to create an edge without a containing node, setup its sources and targets, and call
     * {@link #updateContainment(ElkEdge)} afterwards. 
     * 
     * @param containingNode the edge's containing node. May be {@code null}, in which case the new edge is not added
     *                       to anything.
     * @return the new edge.
     */
    public static ElkEdge createEdge(final ElkNode containingNode) {
        ElkEdge edge = ElkGraphFactory.eINSTANCE.createElkEdge();
        
        if (containingNode != null) {
            edge.setContainingNode(containingNode);
        }
        
        return edge;
    }
    
    /**
     * Creates an edge that connects the given source to the given target and sets the containing node accordingly.
     * This requires the source and target to be in the same graph model.
     * 
     * @param source the edge's source.
     * @param target the edge's target.
     * @return the new edge.
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}.
     */
    public static ElkEdge createSimpleEdge(final ElkConnectableShape source, final ElkConnectableShape target) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(target, "target cannot be null");
        
        ElkEdge edge = createEdge(null);
        
        edge.getSources().add(source);
        edge.getTargets().add(target);
        updateContainment(edge);
        
        return edge;
    }
    
    /**
     * Creates a hyperedge that connects the given sources to the given targets and sets the containing node
     * accordingly. This requires the sources and targets to be in the same graph model.
     * 
     * @param sources the edge's sources.
     * @param targets the edge's targets.
     * @return the new edge.
     * @throws NullPointerException if {@code sources} or {@code targets} is {@code null}.
     */
    public static ElkEdge createHyperedge(final Iterable<ElkConnectableShape> sources,
            final Iterable<ElkConnectableShape> targets) {
        
        Objects.requireNonNull(sources, "sources cannot be null");
        Objects.requireNonNull(targets, "targets cannot be null");
        
        ElkEdge edge = createEdge(null);
        
        List<ElkConnectableShape> edgeSources = edge.getSources();
        for (ElkConnectableShape source : sources) {
            edgeSources.add(source);
        }
        
        List<ElkConnectableShape> edgeTargets = edge.getTargets();
        for (ElkConnectableShape target : targets) {
            edgeTargets.add(target);
        }
        
        updateContainment(edge);
        
        return edge;
    }
    
    /**
     * Changes an edge's containment to the one returned by {@link #findBestEdgeContainment(ElkEdge)}.
     * 
     * @param edge the edge to update the containment of.
     * @throws NullPointerException if {@code edge} is {@code null}.
     * @throws IllegalArgumentException if {@code edge} does not have at least one source or target.
     */
    public static void updateContainment(final ElkEdge edge) {
        Objects.requireNonNull(edge, "edge cannot be null");
        
        edge.setContainingNode(findBestEdgeContainment(edge));
    }
    
    /**
     * Finds the node the given edge should best be contained in given the edge's sources and targets. This is usually
     * the first common ancestor of all sources and targets. Finding this containment requires all sources and targets
     * to be contained in the same graph model. If that is not the case, this method will return {@code null}. If the
     * edge is not connected to anything, an exception is thrown. If the edge is connected to only one shape, that
     * shape's parent is returned.
     * 
     * @param edge the edge to find the best containment for.
     * @return the best containing node, or {@code null} if none could be found.
     * @throws NullPointerException if {@code edge} is {@code null}.
     * @throws IllegalArgumentException if {@code edge} does not have at least one source or target.
     */
    public static ElkNode findBestEdgeContainment(final ElkEdge edge) {
        Objects.requireNonNull(edge, "edge cannot be null");
        
        /* We start with corner cases: an edge which is not connected to anything and an edge which is connected to
         * just one shape.
         */
        
        switch (edge.getSources().size() + edge.getTargets().size()) {
        case 0:
            // We need at least one to work with
            throw new IllegalArgumentException("The edge must have at least one source or target.");
            
        case 1:
            // Return the parent of the only incident thing
            if (edge.getSources().isEmpty()) {
                return connectableShapeToNode(edge.getTargets().get(0)).getParent();
            } else {
                return connectableShapeToNode(edge.getSources().get(0)).getParent();
            }
        }
        
        /* From now on we know that the edge has at least two incident shapes. We now check the simple common cases
         * first before moving on to the more complex general case. The most common cases are that the edge is not a
         * hyperedge and one of the following is true:
         *   1. The edge's source and target have the same parent node. In this case, the containment is that parent
         *      node.
         *   2. The source is the target's parent (or the other way around). In that case, the source or the target
         *      is the containment, respectively.
         */
        
        if (edge.getSources().size() == 1 && edge.getTargets().size() == 1) {
            ElkNode sourceNode = connectableShapeToNode(edge.getSources().get(0));
            ElkNode targetNode = connectableShapeToNode(edge.getTargets().get(0));
            
            if (sourceNode.getParent() == targetNode.getParent()) {
                return sourceNode.getParent();
            } else if (sourceNode == targetNode.getParent()) {
                return sourceNode;
            } else if (targetNode == sourceNode.getParent()) {
                return targetNode;
            }
        }
        
        /* Finally, the most general case. We go through all incident shapes and keep track of the highest common
         * ancestor we have found so far. For each node we process, we check if it is a descendant of the current
         * highest common ancestor. Note how we allow the highest common ancestor to be one of the incident nodes
         * itself. In that case, all nodes encountered so far have been contained in that node.
         */
        
        Iterator<ElkConnectableShape> incidentShapes = allIncidentShapes(edge).iterator();
        ElkNode commonAncestor = connectableShapeToNode(incidentShapes.next());
        
        while (incidentShapes.hasNext()) {
            ElkNode incidentNode = connectableShapeToNode(incidentShapes.next());
            
            // Check if the current common ancester is not an ancestor to the new node, in which case we need to act
            if (!isDescendant(incidentNode, commonAncestor)) {
                if (incidentNode.getParent() == commonAncestor.getParent()) {
                    // The two nodes are siblings, the common ancestor is their parent
                    commonAncestor = incidentNode.getParent();
                } else if (isDescendant(commonAncestor, incidentNode)) {
                    // The new node is the ancestor of everyone we've processed so far
                    commonAncestor = incidentNode;
                }
            }
        }
        
        return commonAncestor;
    }
    
    // TODO Add methods to create edge sections.
    
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Convenience
    
    /**
     * Returns an iterable which contains all edges incoming to a node, whether directly connected or
     * through a port.
     * 
     * @param node the node whose incoming edges to gather.
     * @return iterable with all incoming edges.
     */
    public static Iterable<ElkEdge> allIncomingEdges(final ElkNode node) {
        List<Iterable<ElkEdge>> incomingEdgeIterables = Lists.newArrayListWithCapacity(1 + node.getPorts().size());
        
        incomingEdgeIterables.add(node.getIncomingEdges());
        for (ElkPort port : node.getPorts()) {
            incomingEdgeIterables.add(port.getIncomingEdges());
        }
        
        return Iterables.concat(incomingEdgeIterables);
    }
    
    /**
     * Returns an iterable which contains all edges outgoing from a node, whether directly connected or
     * through a port.
     * 
     * @param node the node whose outgoing edges to gather.
     * @return iterable with all outgoing edges.
     */
    public static Iterable<ElkEdge> allOutgoingEdges(final ElkNode node) {
        List<Iterable<ElkEdge>> outgoingEdgeIterables = Lists.newArrayListWithCapacity(1 + node.getPorts().size());
        
        outgoingEdgeIterables.add(node.getOutgoingEdges());
        for (ElkPort port : node.getPorts()) {
            outgoingEdgeIterables.add(port.getOutgoingEdges());
        }
        
        return Iterables.concat(outgoingEdgeIterables);
    }
    
    /**
     * Returns an iterable which contains all connectable shapes the given edge is incident to.
     * 
     * @param edge the edge whose end points to return.
     * @return iterable with all incident shapes.
     */
    public static Iterable<ElkConnectableShape> allIncidentShapes(final ElkEdge edge) {
        return Iterables.concat(edge.getSources(), edge.getTargets());
    }
    
    /**
     * Gathers all edge sections incident to a given edge section, regardless of whether the sections are
     * incoming or outgoing.
     * 
     * @param section the section whose incident sections to gather.
     * @return iterable over all incident sections.
     */
    public static Iterable<ElkEdgeSection> allIncidentSections(final ElkEdgeSection section) {
        return Iterables.concat(section.getIncomingSections(), section.getOutgoingSections());
    }

    /**
     * Determines whether the given child node is a descendant of the given ancestor. This method is not reflexive (a
     * node is not its own descendant).
     * 
     * @param child a child node.
     * @param ancestor a prospective ancestory node.
     * @return {@code true} if {@code child} is a direct or indirect child of {@code ancestor}.
     */
    public static boolean isDescendant(final ElkNode child, final ElkNode ancestor) {
        // Go up the hierarchy and see if we find the ancestor
        ElkNode current = child;
        while (current.getParent() != null) {
            current = current.getParent();
            if (current == ancestor) {
                return true;
            }
        }
        
        // Reached the root node without finding the ancestor
        return false;
    }
    
    /**
     * Returns the node that belongs to the given connectable shape. That is, if the shape is a node, that itself is
     * returned. If it is a port, the port's parent node is returned. This method may well return {@code null} if the
     * connectable shape is a port that does not belong to a node.
     * 
     * @param connectableShape the shape whose node to return.
     * @return the node that belongs to the shape.
     */
    public static ElkNode connectableShapeToNode(final ElkConnectableShape connectableShape) {
        if (connectableShape instanceof ElkNode) {
            return (ElkNode) connectableShape;
        } else if (connectableShape instanceof ElkPort) {
            return ((ElkPort) connectableShape).getParent();
        } else {
            // In case the meta model is changed in the distant future...
            throw new UnsupportedOperationException("Only support nodes and ports.");
        }
    }
    
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Privates
    
    /**
     * Private constructor, don't call.
     */
    private ElkGraphUtil() {
        // Nothing to do here...
    }
    
}