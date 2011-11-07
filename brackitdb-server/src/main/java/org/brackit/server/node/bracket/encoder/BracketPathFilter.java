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
package org.brackit.server.node.bracket.encoder;

import java.util.List;
import java.util.Set;

import org.brackit.server.metadata.pathSynopsis.manager.PathSynopsisMgr;
import org.brackit.server.node.bracket.BracketCollection;
import org.brackit.server.node.bracket.BracketNode;
import org.brackit.server.tx.Tx;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.node.stream.filter.Filter;
import org.brackit.xquery.util.path.Path;
import org.brackit.xquery.xdm.DocumentException;

/**
 * @author Sebastian Baechle
 *
 */
public class BracketPathFilter implements Filter<BracketNode> {

	private final List<Path<QNm>> paths;

	private final Tx tx;

	private final PathSynopsisMgr pathSynopsis;

	private final boolean genericPath;

	private Set<Integer> pcrFilter;

	private int maxKnownPCR;

	public BracketPathFilter(List<Path<QNm>> paths, BracketCollection collection) {
		super();
		this.paths = paths;
		this.tx = collection.getTX();
		this.pathSynopsis = collection.getPathSynopsis();
		this.genericPath = paths.isEmpty();
		this.maxKnownPCR = -1;
	}

	@Override
	public boolean filter(BracketNode node) throws DocumentException {
		if (genericPath) {
			return false;
		}

		int pcr = node.getPCR();

		if (pcr > maxKnownPCR) {
			maxKnownPCR = pathSynopsis.getMaxPCR();
			pcrFilter = pathSynopsis.getPCRsForPaths(paths);
		}

		return (!pcrFilter.contains(pcr));
	}
}
