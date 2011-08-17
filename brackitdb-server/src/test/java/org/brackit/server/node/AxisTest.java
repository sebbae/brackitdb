/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.server.node;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.brackit.xquery.util.log.Logger;
import org.brackit.server.XQueryBaseTest;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.expr.Accessor;
import org.brackit.xquery.node.SubtreePrinter;
import org.brackit.xquery.node.stream.StreamUtil;
import org.brackit.xquery.node.stream.filter.Filter;
import org.brackit.xquery.node.stream.filter.FilteredStream;
import org.brackit.xquery.xdm.Axis;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;
import org.junit.Test;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public abstract class AxisTest extends XQueryBaseTest {
	private static final Logger log = Logger
			.getLogger(AxisTest.class.getName());

	private static final Comparator<Node<?>> COMPARATOR = new Comparator<Node<?>>() {
		@Override
		public int compare(Node<?> o1, Node<?> o2) {
			// System.out.println(o1 + " cmp " + o2 + ": " + o1.cmp(o2));
			return o1.cmp(o2);
		}
	};;

	private Collection<?> collection;

	private class AxisFilter implements Filter<Node<?>> {
		private final QueryContext ctx;

		private final Node<?> node;

		private final Axis axis;

		public AxisFilter(QueryContext ctx, Node<?> node, Axis axis) {
			this.ctx = ctx;
			this.node = node;
			this.axis = axis;
		}

		public boolean filter(Node<?> element) throws DocumentException {
			try {
				boolean check = !axis.check(element, node);

				if (check) {
					System.err.println("Filter out " + element + " -> !" + axis
							+ " of " + node);
				} else {
					System.out.println("Accept " + element + " -> " + axis
							+ " of " + node);
				}
				//				
				return check;
			} catch (QueryException e) {
				throw new DocumentException(e);
			}
		}
	}

	@Test
	public void testCmp() throws Exception {
		List<? extends Node<?>> nodes = new ArrayList(StreamUtil
				.asList(collection.getDocument().getSubtree()));
		for (int i = 0; i < nodes.size(); i++) {
			Node<?> a = nodes.get(i);
			for (int j = 0; j < nodes.size(); j++) {
				Node<?> b = nodes.get(j);
				try {
					if (i < j)
						Assert.assertTrue("a < b", a.cmp(b) < 0);
					else if (i == j)
						Assert.assertTrue("a == b", a.cmp(b) == 0);
					else
						Assert.assertTrue("a > b", a.cmp(b) > 0);
				} catch (AssertionError e) {
					SubtreePrinter.print(collection.getDocument(), System.out);
					System.err.println(nodes);
					System.err.println(a);
					System.err.println(b);
					System.err.println(a.cmp(b));
					throw e;
				}
			}
		}
	}

	@Test
	public void testRootElementChildren() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(), new AxisFilter(ctx, node, Axis.CHILD));
		checkOutput(Accessor.CHILD.performStep(node), expected);
	}

	@Test
	public void testNonRootElementChildren() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild().getFirstChild();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(), new AxisFilter(ctx, node, Axis.CHILD));
		checkOutput(Accessor.CHILD.performStep(node), expected);
	}

	@Test
	public void testRootFollowing() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(), new AxisFilter(ctx, node, Axis.FOLLOWING));
		checkOutput(Accessor.FOLLOWING.performStep(node), expected);
	}

	@Test
	public void testNonRootFollowing() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild().getFirstChild()
				.getFirstChild().getNextSibling();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(), new AxisFilter(ctx, node, Axis.FOLLOWING));
		checkOutput(Accessor.FOLLOWING.performStep(node), expected);
	}

	@Test
	public void testRootPreceding() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(), new AxisFilter(ctx, node, Axis.PRECEDING));
		checkOutput(Accessor.PRECEDING.performStep(node), expected);
	}

	@Test
	public void testNonRootPreceding() throws Exception {
		SubtreePrinter.print(collection.getDocument(), System.out);
		Node<?> node = collection.getDocument().getFirstChild().getFirstChild()
				.getFirstChild().getNextSibling();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(), new AxisFilter(ctx, node, Axis.PRECEDING));
		checkOutput(Accessor.PRECEDING.performStep(node), expected);
	}

	@Test
	public void testRootPrecedingSibling() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(),
				new AxisFilter(ctx, node, Axis.PRECEDING_SIBLING));
		checkOutput(Accessor.PRECEDING_SIBLING.performStep(node), expected);
	}

	@Test
	public void testNonRootPrecedingSibling() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild().getFirstChild()
				.getFirstChild().getNextSibling();
		System.out.println(node);
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(),
				new AxisFilter(ctx, node, Axis.PRECEDING_SIBLING));
		System.out.println("---------------");
		checkOutput(Accessor.PRECEDING_SIBLING.performStep(node), expected);
	}

	@Test
	public void testRootFollowingSibling() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(),
				new AxisFilter(ctx, node, Axis.FOLLOWING_SIBLING));
		checkOutput(Accessor.FOLLOWING_SIBLING.performStep(node), expected);
	}

	@Test
	public void testNonRootFollowingSibling() throws Exception {
		Node<?> node = collection.getDocument().getFirstChild().getFirstChild()
				.getFirstChild().getNextSibling();
		Set<Node<?>> expected = buildExpectedSet(collection.getDocument()
				.getSubtree(),
				new AxisFilter(ctx, node, Axis.FOLLOWING_SIBLING));
		checkOutput(Accessor.FOLLOWING_SIBLING.performStep(node), expected);
	}

	protected Set<Node<?>> buildExpectedSet(
			final Stream<? extends Node<?>> original, Filter<Node<?>> filter)
			throws DocumentException {
		TreeSet<Node<?>> expected = new TreeSet<Node<?>>(COMPARATOR);
		Stream<? extends Node<?>> stream = original;

		if (filter != null) {
			stream = new FilteredStream<Node<?>>(original, filter);
		}

		Node<?> next;
		while ((next = stream.next()) != null) {
			expected.add(next);
		}
		stream.close();
		return expected;
	}

	protected void checkOutput(Stream<? extends Node<?>> nodes,
			Set<Node<?>> expected) throws Exception {
		TreeSet<Node<?>> delivered = new TreeSet<Node<?>>(COMPARATOR);
		Node<?> node;
		while ((node = nodes.next()) != null) {
			Assert.assertTrue("Node not delivered yet.", delivered.add(node));
			System.out.println(node);
		}
		nodes.close();
		try {
			Assert.assertEquals("Expected number of nodes delivered", expected
					.size(), delivered.size());

			for (Node<?> n : delivered) {
				System.err.println("CHECKING " + n);
				if (!expected.contains(n)) {
					System.err.println(n + " is not contained in " + expected);
					System.err.println("Expected:\t" + expected);
					System.err.println("Delivered:\t" + delivered);
					return;
				}
			}

			Assert.assertTrue("Expected nodes delivered", expected
					.containsAll(delivered));
		} catch (Error e) {
			System.out.println("Expected:\t" + expected);
			System.out.println("Delivered:\t" + delivered);
			throw e;
		}
	}

	@Override
	public void setUp() throws Exception, FileNotFoundException {
		super.setUp();
		collection = storeFile("text.xml", "/docs/orga.xml");
	}
}