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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.node.SubtreePrinter;
import org.brackit.xquery.node.parser.DocumentParser;
import org.brackit.xquery.node.parser.StreamSubtreeProcessor;
import org.brackit.xquery.node.parser.SubtreeListener;
import org.brackit.xquery.xdm.Collection;
import org.brackit.xquery.xdm.DocumentException;
import org.brackit.xquery.xdm.Kind;
import org.brackit.xquery.xdm.Node;
import org.brackit.xquery.xdm.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

public abstract class NodeTest<E extends Node<E>> {
	protected static final Logger log = Logger.getLogger(NodeTest.class
			.getName());

	protected static final String DOCUMENT = "<?xml version = '1.0' encoding = 'UTF-8'?>"
			+ "<Organization>"
			+ "<Department>"
			+ "<Member key=\"12\" employee=\"true\">"
			+ "<Firstname>Kurt</Firstname>"
			+ "<Lastname>Mayer</Lastname>"
			+ "<DateOfBirth>1.4.1963</DateOfBirth>"
			+ "<Title>Dr.-Ing.</Title>"
			+ "</Member>"
			+ "<Member key=\"40\"  employe=\"false\">"
			+ "<Firstname>Hans</Firstname>"
			+ "<Lastname>Mettmann</Lastname>"
			+ "<DateOfBirth>12.9.1974</DateOfBirth>"
			+ "<Title>Dipl.-Inf</Title>"
			+ "</Member>"
			+ "<Member>"
			+ "</Member>"
			+ "<Member>"
			+ "</Member>"
			+ "</Department>"
			+ "<Project id=\"4711\" priority=\"high\">"
			+ "<Title>XML-DB</Title>"
			+ "<Budget>10000</Budget>"
			+ "</Project>"
			+ "<Project id=\"666\" priority=\"evenhigher\">"
			+ "<Title>DISS</Title>"
			+ "<Budget>7000</Budget>"
			+ "<Abstract>Native<b>XML</b>-Databases</Abstract>"
			+ "</Project>"
			+ "</Organization>";

	protected static final String ROOT_ONLY_DOCUMENT = "<?xml version = '1.0' encoding = 'UTF-8'?><root/>";

	protected QueryContext ctx;

	protected Random rand;

	@Test
	public void testStoreDocument() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));
	}

	@Test
	public void testGetFirstChildForDocumentNode() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(
				"<a><b/><c/></a>"));
		assertEquals("First child is document root node", coll.getDocument()
				.getFirstChild(), coll.getDocument().getFirstChild());
	}

	@Test
	public void testGetLastChildForDocumentNode() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(
				"<a><b/><c/></a>"));
		assertEquals("Last child is document root node", coll.getDocument()
				.getFirstChild(), coll.getDocument().getLastChild());
	}

	@Test
	public void testGetChildrenForDocumentNode() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(
				"<a><b/><c/></a>"));

		Stream<? extends E> children = coll.getDocument().getChildren();
		E n;
		assertNotNull("Document node has a child node", n = children.next());
		assertEquals("First child is document root node", coll.getDocument()
				.getFirstChild(), n);
		assertNull("Document node no further children", n = children.next());
		children.close();
	}

	@Test
	public void testGetSubtreeForDocumentNode() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(
				"<a><b/><c/></a>"));

		Stream<? extends E> subtree = coll.getDocument().getSubtree();

		E n;
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("First node is document node", coll.getDocument(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("Second node is document root node", coll.getDocument()
				.getFirstChild(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("Third node is document root node's first child", coll
				.getDocument().getFirstChild().getFirstChild(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("Fourth node is document root node's last child", coll
				.getDocument().getFirstChild().getLastChild(), n);
		subtree.close();
	}

	@Test
	public void testGetSubtreeForRootNode() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(
				"<a><b/><c/></a>"));

		Stream<? extends E> subtree = coll.getDocument().getFirstChild()
				.getSubtree();

		E n;
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("First node is document root node", coll.getDocument()
				.getFirstChild(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("Second node is document root node's first child", coll
				.getDocument().getFirstChild().getFirstChild(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("Third node is document root node's last child", coll
				.getDocument().getFirstChild().getLastChild(), n);
		subtree.close();
	}

	@Test
	public void testGetSubtreeForNonRootNode() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(
				"<a><b><d/><e/></b><c/></a>"));

		Stream<? extends E> subtree = coll.getDocument().getFirstChild()
				.getFirstChild().getSubtree();

		E n;
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals("First node is document root node", coll.getDocument()
				.getFirstChild().getFirstChild(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals(
				"Second node is document root node's first child first child",
				coll.getDocument().getFirstChild().getFirstChild()
						.getFirstChild(), n);
		assertNotNull("Stream not empty", n = subtree.next());
		assertEquals(
				"Third node is document root node's first child last child",
				coll.getDocument().getFirstChild().getFirstChild()
						.getLastChild(), n);
		subtree.close();
	}

	@Test
	public void traverseDocumentInPreorder() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));
		E root = coll.getDocument().getFirstChild();
		org.w3c.dom.Node domRoot = null;

		domRoot = createDomTree(ctx,
				new InputSource(new StringReader(DOCUMENT)));

		checkSubtreePreOrder(ctx, root, domRoot); // check document index
	}

	protected org.w3c.dom.Node createDomTree(QueryContext ctx,
			InputSource source) throws Exception {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(source);
			org.w3c.dom.Node root = document.getDocumentElement();
			removeWhitespaceNodes(root);
			return root;
		} catch (Exception e) {
			throw new DocumentException(
					"An error occured while creating DOM input source: %s",
					e.getMessage());
		}
	}

	/**
	 * Removes text nodes that contain only whitespace (e.g. spaces, newlines)
	 * from the given tree.
	 * 
	 * @param node
	 *            the subtree root
	 * @return true if current node was deleted
	 */
	private boolean removeWhitespaceNodes(org.w3c.dom.Node node) {
		if (node == null) {
			return false;
		}
		if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
			String trimmed = node.getTextContent().trim();
			if (trimmed.isEmpty()) {
				node.getParentNode().removeChild(node);
				return true;
			} else {
				node.setNodeValue(trimmed);
			}
		} else if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (removeWhitespaceNodes(children.item(i))) {
					i--; // child deleted, step one back
				}
			}
		}
		return false;
	}

	protected void checkSubtreePreOrder(QueryContext ctx, final E node,
			org.w3c.dom.Node domNode) throws Exception {
		E child = null;
		String nodeString = node.toString();

		if (domNode instanceof Element) {
			Element element = (Element) domNode;
			assertEquals(nodeString + " is of type element", Kind.ELEMENT,
					node.getKind());

			// System.out.println("Checking name of element " +
			// node.getDeweyID() + " level " + node.getDeweyID().getLevel() +
			// " is " + element.getNodeName());

			assertEquals(String.format("Name of node %s", nodeString),
					element.getNodeName(), node.getName());
			compareAttributes(ctx, node, element);

			NodeList domChildNodes = element.getChildNodes();
			ArrayList<E> children = new ArrayList<E>();

			for (E c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
				// System.out.println(String.format("-> Found child of %s : %s",
				// node, c));
				String cString = c.toString();

				int ancestorLevel = 0;
				for (E ancestor = node; ancestor != null; ancestor = ancestor
						.getParent()) {
					String ancestorString = ancestor.toString();
					if (ancestorLevel == 0) {
						assertTrue(String.format("node %s is child of %s", cString,
								ancestorString), c.isChildOf(ancestor));
						assertTrue(String.format("node %s is parent of %s",
								ancestorString, cString), ancestor.isParentOf(c));
					}
					assertTrue(String.format("node %s is descendant of %s", cString,
							ancestorString), c.isDescendantOf(ancestor));
					assertTrue(String.format("node %s is ancestor of %s",
							ancestorString, cString), ancestor.isAncestorOf(c));
					ancestorLevel++;
				}

				for (E sibling : children) {
					String siblingString = sibling.toString();
					assertTrue(String.format("node %s is sibling of %s", cString,
							siblingString), c.isSiblingOf(sibling));
					assertTrue(String.format("node %s is sibling of %s",
							siblingString, cString), sibling.isSiblingOf(c));
					assertTrue(String.format(
							"node %s is preceding sibling of %s", siblingString, cString),
							sibling.isPrecedingSiblingOf(c));
					assertTrue(String.format(
							"node %s is following sibling of %s", cString, siblingString),
							c.isFollowingSiblingOf(sibling));
					assertTrue(String.format("node %s is preceding of %s",
							siblingString, cString), sibling.isPrecedingOf(c));
					assertTrue(String.format("node %s is following of %s", cString,
							siblingString), c.isFollowingOf(sibling));

					try {
						assertFalse(String.format(
								"node %s is not preceding sibling of %s", cString,
								siblingString), c.isPrecedingSiblingOf(sibling));
					} catch (AssertionError e) {
						c.isPrecedingSiblingOf(sibling);
						throw e;
					}
					assertFalse(String.format(
							"node %s is following sibling of %s", siblingString, cString),
							sibling.isFollowingSiblingOf(c));

					assertFalse(String.format("node %s is not preceding of %s",
							cString, siblingString), c.isPrecedingOf(sibling));
					assertFalse(String.format("node %s is following of %s",
							siblingString, cString), sibling.isFollowingOf(c));
				}

				children.add(c);
			}

			for (int i = 0; i < domChildNodes.getLength(); i++) {
				org.w3c.dom.Node domChild = domChildNodes.item(i);
				// System.out.println("Checking if child  " + ((domChild
				// instanceof Element) ? domChild.getNodeName() :
				// domChild.getNodeValue()) + " exists under " + node);

				if (child == null) {
					child = node.getFirstChild();
					// System.out.println(String.format("First child of %s is %s",
					// node, child));
				} else {
					child = child.getNextSibling();
					// System.out.println(String.format("Next sibling of %s is %s",
					// oldChild, child));
				}

				assertNotNull(
						String.format("child node %s of node %s", i, nodeString),
						child);

				checkSubtreePreOrder(ctx, child, domChild);
			}

			assertEquals(String.format("child count of element %s", nodeString),
					domChildNodes.getLength(), children.size());

		} else if (domNode instanceof Text) {
			Text text = (Text) domNode;

			assertEquals(nodeString + " is of type text : \"" + text.getNodeValue()
					+ "\"", Kind.TEXT, node.getKind());
			assertEquals(String.format("Text of node %s", nodeString), text
					.getNodeValue().trim(), node.getValue());
		} else {
			throw new DocumentException("Unexpected dom node: %s",
					domNode.getClass());
		}
	}
	
	protected void checkSubtreePreOrderReduced(QueryContext ctx, final E node,
			org.w3c.dom.Node domNode) throws Exception {
		E child = null;
		String nodeString = node.toString();

		if (domNode instanceof Element) {
			Element element = (Element) domNode;
			assertEquals(nodeString + " is of type element", Kind.ELEMENT,
					node.getKind());

			// System.out.println("Checking name of element " +
			// node.getDeweyID() + " level " + node.getDeweyID().getLevel() +
			// " is " + element.getNodeName());

			assertEquals(String.format("Name of node %s", nodeString),
					element.getNodeName(), node.getName());
			compareAttributes(ctx, node, element);

			NodeList domChildNodes = element.getChildNodes();
			ArrayList<E> children = new ArrayList<E>();

			for (E c = node.getFirstChild(); c != null; c = c.getNextSibling()) {
				// System.out.println(String.format("-> Found child of %s : %s",
				// node, c));
				children.add(c);
			}

			for (int i = 0; i < domChildNodes.getLength(); i++) {
				org.w3c.dom.Node domChild = domChildNodes.item(i);
				// System.out.println("Checking if child  " + ((domChild
				// instanceof Element) ? domChild.getNodeName() :
				// domChild.getNodeValue()) + " exists under " + node);

				if (child == null) {
					child = node.getFirstChild();
					// System.out.println(String.format("First child of %s is %s",
					// node, child));
				} else {
					child = child.getNextSibling();
					// System.out.println(String.format("Next sibling of %s is %s",
					// oldChild, child));
				}

				assertNotNull(
						String.format("child node %s of node %s", i, nodeString),
						child);

				checkSubtreePreOrderReduced(ctx, child, domChild);
			}

			assertEquals(String.format("child count of element %s", nodeString),
					domChildNodes.getLength(), children.size());

		} else if (domNode instanceof Text) {
			Text text = (Text) domNode;

			assertEquals(nodeString + " is of type text : \"" + text.getNodeValue()
					+ "\"", Kind.TEXT, node.getKind());
			assertEquals(String.format("Text of node %s", nodeString), text
					.getNodeValue().trim(), node.getValue());
		} else {
			throw new DocumentException("Unexpected dom node: %s",
					domNode.getClass());
		}
	}
	
	protected void checkSubtreePostOrder(QueryContext ctx, final E node,
			org.w3c.dom.Node domNode) throws Exception {
		E child = null;
		String nodeString = node.toString();

		if (domNode instanceof Element) {
			Element element = (Element) domNode;
			assertEquals(nodeString + " is of type element", Kind.ELEMENT,
					node.getKind());

			// System.out.println("Checking name of element " +
			// node.getDeweyID() + " level " + node.getDeweyID().getLevel() +
			// " is " + element.getNodeName());

			assertEquals(String.format("Name of node %s", nodeString),
					element.getNodeName(), node.getName());
			compareAttributes(ctx, node, element);

			NodeList domChildNodes = element.getChildNodes();
			ArrayList<E> children = new ArrayList<E>();

			for (E c = node.getLastChild(); c != null; c = c.getPreviousSibling()) {
				// System.out.println(String.format("-> Found child of %s : %s",
				// node, c));
				String cString = c.toString();

				int ancestorLevel = 0;
				for (E ancestor = node; ancestor != null; ancestor = ancestor
						.getParent()) {
					String ancestorString = ancestor.toString();
					if (ancestorLevel == 0) {
						assertTrue(String.format("node %s is child of %s", cString,
								ancestorString), c.isChildOf(ancestor));
						assertTrue(String.format("node %s is parent of %s",
								ancestorString, cString), ancestor.isParentOf(c));
					}
					assertTrue(String.format("node %s is descendant of %s", cString,
							ancestorString), c.isDescendantOf(ancestor));
					assertTrue(String.format("node %s is ancestor of %s",
							ancestorString, cString), ancestor.isAncestorOf(c));
					ancestorLevel++;
				}

				for (E sibling : children) {
					String siblingString = sibling.toString();
					assertTrue(String.format("node %s is sibling of %s", cString,
							siblingString), c.isSiblingOf(sibling));
					assertTrue(String.format("node %s is sibling of %s",
							siblingString, cString), sibling.isSiblingOf(c));
					assertTrue(String.format(
							"node %s is preceding sibling of %s", siblingString, cString),
							sibling.isPrecedingSiblingOf(c));
					assertTrue(String.format(
							"node %s is following sibling of %s", cString, siblingString),
							c.isFollowingSiblingOf(sibling));
					assertTrue(String.format("node %s is preceding of %s",
							siblingString, cString), sibling.isPrecedingOf(c));
					assertTrue(String.format("node %s is following of %s", cString,
							siblingString), c.isFollowingOf(sibling));

					try {
						assertFalse(String.format(
								"node %s is not preceding sibling of %s", cString,
								siblingString), c.isPrecedingSiblingOf(sibling));
					} catch (AssertionError e) {
						c.isPrecedingSiblingOf(sibling);
						throw e;
					}
					assertFalse(String.format(
							"node %s is following sibling of %s", siblingString, cString),
							sibling.isFollowingSiblingOf(c));

					assertFalse(String.format("node %s is not preceding of %s",
							cString, siblingString), c.isPrecedingOf(sibling));
					assertFalse(String.format("node %s is following of %s",
							siblingString, cString), sibling.isFollowingOf(c));
				}

				children.add(c);
			}

			for (int i = domChildNodes.getLength() - 1; i >= 0; i--) {
				org.w3c.dom.Node domChild = domChildNodes.item(i);
				// System.out.println("Checking if child  " + ((domChild
				// instanceof Element) ? domChild.getNodeName() :
				// domChild.getNodeValue()) + " exists under " + node);

				if (child == null) {
					child = node.getLastChild();
					// System.out.println(String.format("First child of %s is %s",
					// node, child));
				} else {
					child = child.getPreviousSibling();
					// System.out.println(String.format("Next sibling of %s is %s",
					// oldChild, child));
				}

				assertNotNull(
						String.format("child node %s of node %s", i, nodeString),
						child);

				checkSubtreePostOrder(ctx, child, domChild);
			}

			assertEquals(String.format("child count of element %s", nodeString),
					domChildNodes.getLength(), children.size());

		} else if (domNode instanceof Text) {
			Text text = (Text) domNode;

			assertEquals(nodeString + " is of type text : \"" + text.getNodeValue()
					+ "\"", Kind.TEXT, node.getKind());
			assertEquals(String.format("Text of node %s", nodeString), text
					.getNodeValue().trim(), node.getValue());
		} else {
			throw new DocumentException("Unexpected dom node: %s",
					domNode.getClass());
		}
	}
	
	protected void checkSubtreePostOrderReduced(QueryContext ctx, final E node,
			org.w3c.dom.Node domNode) throws Exception {
		E child = null;
		String nodeString = node.toString();

		if (domNode instanceof Element) {
			Element element = (Element) domNode;
			assertEquals(nodeString + " is of type element", Kind.ELEMENT,
					node.getKind());

			// System.out.println("Checking name of element " +
			// node.getDeweyID() + " level " + node.getDeweyID().getLevel() +
			// " is " + element.getNodeName());

			assertEquals(String.format("Name of node %s", nodeString),
					element.getNodeName(), node.getName());
			compareAttributes(ctx, node, element);

			NodeList domChildNodes = element.getChildNodes();
			ArrayList<E> children = new ArrayList<E>();

			for (E c = node.getLastChild(); c != null; c = c.getPreviousSibling()) {
				// System.out.println(String.format("-> Found child of %s : %s",
				// node, c));
				children.add(c);
			}

			for (int i = domChildNodes.getLength() - 1; i >= 0; i--) {
				org.w3c.dom.Node domChild = domChildNodes.item(i);
				// System.out.println("Checking if child  " + ((domChild
				// instanceof Element) ? domChild.getNodeName() :
				// domChild.getNodeValue()) + " exists under " + node);

				if (child == null) {
					child = node.getLastChild();
					// System.out.println(String.format("First child of %s is %s",
					// node, child));
				} else {
					child = child.getPreviousSibling();
					// System.out.println(String.format("Next sibling of %s is %s",
					// oldChild, child));
				}

				assertNotNull(
						String.format("child node %s of node %s", i, nodeString),
						child);

				checkSubtreePostOrderReduced(ctx, child, domChild);
			}

			assertEquals(String.format("child count of element %s", nodeString),
					domChildNodes.getLength(), children.size());

		} else if (domNode instanceof Text) {
			Text text = (Text) domNode;

			assertEquals(nodeString + " is of type text : \"" + text.getNodeValue()
					+ "\"", Kind.TEXT, node.getKind());
			assertEquals(String.format("Text of node %s", nodeString), text
					.getNodeValue().trim(), node.getValue());
		} else {
			throw new DocumentException("Unexpected dom node: %s",
					domNode.getClass());
		}
	}

	@Test
	public void traverseDocumentInPostorder() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));
		E root = coll.getDocument().getFirstChild();
		org.w3c.dom.Node domRoot = null;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(new InputSource(new StringReader(
					DOCUMENT)));
			domRoot = document.getDocumentElement();
		} catch (Exception e) {
			throw new DocumentException(
					"An error occured while creating DOM input source: %s",
					e.getMessage());
		}

		checkSubtreePostOrder(ctx, root, domRoot); // check document index
	}

	protected void compareAttributes(QueryContext ctx, E node, Element element)
			throws Exception {
		NamedNodeMap domAttributes = element.getAttributes();
		Stream<? extends E> attributes = node.getAttributes();

		int attributesSize = 0;
		E c;
		while ((c = attributes.next()) != null) {
			attributesSize++;

			int ancestorLevel = 0;
			for (E ancestor = node; ancestor != null; ancestor = ancestor
					.getParent()) {
				if (ancestorLevel == 0) {
					try {
						assertTrue(String.format("node %s is attribute of %s",
								c, ancestor), c.isAttributeOf(ancestor));
					} catch (AssertionError e) {
						c.isAttributeOf(ancestor);
						throw e;
					}
					assertTrue(String.format("node %s is parent of %s",
							ancestor, c), ancestor.isParentOf(c));
				}
				assertTrue(
						String.format("node %s is ancestor of %s", ancestor, c),
						ancestor.isAncestorOf(c));
				ancestorLevel++;
			}
		}
		attributes.close();

		assertEquals(String.format("attribute count of element %s", node),
				domAttributes.getLength(), attributesSize);

		// check if all stored attributes really exist
		for (int i = 0; i < domAttributes.getLength(); i++) {
			Attr domAttribute = (Attr) domAttributes.item(i);
			E attribute = node.getAttribute(domAttribute.getName());
			assertNotNull(
					String.format("Attribute \"%s\" of node %s",
							domAttribute.getName(), node), attribute);
			assertEquals(attribute + " is of type attribute", Kind.ATTRIBUTE,
					attribute.getKind());
			assertEquals(String.format(
					"Value of attribute \"%s\" (%s) of node %s",
					domAttribute.getName(), attribute, node),
					domAttribute.getValue(), attribute.getValue());
		}
	}

	@Test
	public void testScanDocument() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));

		Stream<? extends E> stream = coll.getDocument().getFirstChild()
				.getSubtree();
		E next;
		while ((next = stream.next()) != null) {
			System.out.println(next);
		}
		stream.close();
	}

	@Test
	public void testProcessSubtree() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));

		System.err.println("-------------------");
		Stream<? extends E> stream = coll.getDocument().getFirstChild()
				.getSubtree();
		StreamSubtreeProcessor<? extends E> processor = new StreamSubtreeProcessor(
				stream, new SubtreeListener[] { new SubtreePrinter(System.out,
						true) });
		processor.process();
	}

	@Test
	public void testInsertSubtree() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));

		E root = coll.getDocument().getFirstChild();
		E node = root.getFirstChild();
		node = node.getFirstChild();
		node = node.getNextSibling();
		node = node.getNextSibling();
		DocumentParser docParser = new DocumentParser("<test><a/><b/></test>");
		docParser.setParseAsFragment(true);
		node.append(docParser);
	}

	@Test
	public void testInsertRecord() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));

		E root = coll.getDocument().getFirstChild();
		E lastChild = root.getLastChild();
		E newLastChild = root.append(Kind.ELEMENT, "Project");
	}

	@Test
	public void testSetAttribute() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));

		E root = coll.getDocument().getFirstChild();
		E node = root.getFirstChild();
		node = root.getFirstChild();
		node = root.getFirstChild();
		node = node.getNextSibling();
		node = node.getNextSibling();
		node.setAttribute("new", "4711");
		E attribute = node.getAttribute("new");
	}

	@Test
	public void testGetText() throws Exception {
		Collection<E> coll = createDocument(new DocumentParser(DOCUMENT));
		E root = coll.getDocument().getFirstChild();
		System.out.println(root.getValue());
		E node = root.getFirstChild();
		System.out.println(node.getValue());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		ctx = new QueryContext(null);

		// use same random source to get reproducible results in case of an
		// error
		rand = new Random(12345678);
	}

	protected abstract Collection<E> createDocument(
			DocumentParser documentParser) throws Exception;
}
