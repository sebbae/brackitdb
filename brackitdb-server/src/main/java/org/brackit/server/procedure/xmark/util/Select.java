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
package org.brackit.server.procedure.xmark.util;

import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.operator.Cursor;
import org.brackit.xquery.xdm.Expr;

/**
 * @author Sebastian Baechle
 * 
 */
public class Select implements Cursor {
	private final Cursor in;

	private final Expr predicate;

	private final int[] projections;

	public Select(Cursor in, Expr predicate) {
		this(in, predicate, null);
	}

	public Select(Cursor in, Expr predicate, int... projections) {
		this.in = in;
		this.predicate = predicate;
		this.projections = projections;
	}

	@Override
	public void close(QueryContext ctx) {
		in.close(ctx);
	}

	@Override
	public Tuple next(QueryContext ctx) throws QueryException {
		Tuple a;
		while (((a = in.next(ctx)) != null)
				&& (!predicate.evaluate(ctx, a).booleanValue()))
			;
		return (a != null) ? (projections != null) ? a.project(projections)
				: a : null;
	}

	@Override
	public void open(QueryContext ctx) throws QueryException {
		in.open(ctx);
	}
}