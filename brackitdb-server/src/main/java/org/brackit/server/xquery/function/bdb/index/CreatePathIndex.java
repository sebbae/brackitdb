/*
 * [New BSD License]
 * Copyright (c) 2011-2012, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Brackit Project Team nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.server.xquery.function.bdb.index;

import java.util.LinkedList;
import java.util.List;

import org.brackit.server.metadata.DBCollection;
import org.brackit.server.metadata.TXQueryContext;
import org.brackit.server.node.index.definition.IndexDef;
import org.brackit.server.node.index.definition.IndexDefBuilder;
import org.brackit.server.xquery.function.bdb.BDBFun;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.module.StaticContext;
import org.brackit.xquery.util.path.Path;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Signature;

/**
 * Function for creating path indexes on stored documents, optionally restricted
 * to a set of paths. If successful, this function returns statistics about the
 * newly created index as an XML fragment. Supported signatures are:</br>
 * <ul>
 * <li>
 * <code>bdb:create-path-index($coll as xs:string, $paths as xs:string*) as 
 * node()</code></li>
 * <li><code>bdb:create-path-index($coll as xs:string) as node()</code></li>
 * </ul>
 * 
 * @author Max Bechtold
 * 
 */
public class CreatePathIndex extends AbstractFunction {

	public final static QNm CREATE_PATH_INDEX = new QNm(BDBFun.BDB_NSURI,
			BDBFun.BDB_PREFIX, "create-path-index");

	public CreatePathIndex(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(StaticContext sctx, QueryContext ctx,
			Sequence[] args) throws QueryException {
		TXQueryContext txCtx = (TXQueryContext) ctx;
		DBCollection<?> col = (DBCollection<?>) txCtx.getStore().lookup(
				((Str) args[0]).str);
		List<Path<QNm>> paths = new LinkedList<Path<QNm>>();
		if (args.length > 1 && args[1] != null) {
			Iter it = args[1].iterate();
			Item next = it.next();
			while (next != null) {
				paths.add(Path.parse(((Str) next).str));
				next = it.next();
			}
		}

		IndexDef idxDef = IndexDefBuilder.createPathIdxDef(null, paths);
		col.getIndexController().createIndexes(idxDef);
		return idxDef.materialize();
	}

}
