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
package org.brackit.server.tx.locking.protocol;

import java.util.Arrays;
import java.util.Collection;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class RIX implements
		TreeLockProtocol<org.brackit.server.tx.locking.protocol.RIX.Mode> {
	public enum Mode implements TreeLockMode<Mode> {
		I(0) {
			@Override
			public Mode escalate(int distanceToTargetLevel) {
				return X;
			}

			@Override
			public Mode implicitlyGrantedChildMode() {
				return null;
			}

			@Override
			public Mode requiredParentMode() {
				return I;
			}
		},

		R(1) {
			@Override
			public Mode escalate(int distanceToTargetLevel) {
				return R;
			}

			@Override
			public Mode implicitlyGrantedChildMode() {
				return null;
			}

			@Override
			public Mode requiredParentMode() {
				return I;
			}
		},

		X(2) {
			@Override
			public Mode escalate(int distanceToTargetLevel) {
				return X;
			}

			@Override
			public Mode implicitlyGrantedChildMode() {
				return null;
			}

			@Override
			public Mode requiredParentMode() {
				return I;
			}
		};

		private static final boolean YE = true;

		private static final boolean NO = false;

		public static final boolean[][] compatibility = {
		/* I R X */
		/* I */{ YE, NO, NO },
		/* R */{ NO, YE, NO },
		/* X */{ NO, NO, NO } };

		public static final Mode[][] conversions = {
		/* I R X */
		/* I */{ I, X, X },
		/* R */{ X, R, X },
		/* X */{ X, X, X } };

		private final int id;

		private Mode(int id) {
			this.id = id;
		}

		@Override
		public abstract Mode escalate(int distanceToTargetLevel);

		@Override
		public abstract Mode implicitlyGrantedChildMode();

		@Override
		public abstract Mode requiredParentMode();

		public Mode convert(Mode requestedMode) {
			return conversions[requestedMode.id][id];
		}

		public boolean isCompatible(Mode requestedMode) {
			return (compatibility[requestedMode.id][id]);
		}

		public boolean implies(Mode targetMode, int distanceToTargetLevel) {
			return false;
		}

		public Mode requiredAncestorMode(int distance) {
			Mode ancestorMode = this;

			for (int i = 0; i < distance; i++) {
				ancestorMode = ancestorMode.requiredParentMode();
			}

			return ancestorMode;
		}

		public Mode implicitMode(int distance) {
			Mode descendantMode = this;

			for (int i = 0; i < distance; i++) {
				descendantMode = descendantMode.implicitlyGrantedChildMode();
			}

			return descendantMode;
		}
	}

	public Mode getLevelSharedMode() {
		return null;
	}

	public Mode getNodeExclusiveMode() {
		return Mode.X;
	}

	public Mode getNodeSharedMode() {
		return Mode.I;
	}

	public Mode getNodeUpdateMode() {
		return Mode.X;
	}

	public Mode getTreeExclusiveMode() {
		return Mode.X;
	}

	public Mode getTreeSharedMode() {
		return Mode.R;
	}

	public Mode getTreeUpdateMode() {
		return Mode.X;
	}

	public Collection<Mode> modes() {
		return Arrays.asList(Mode.values());
	}

	public final static void main(String[] args) {
		System.out.println(LockProtocolUtil.dump(new RIX()));
		LockProtocolUtil.testConversionMatrix(new RIX());
	}
}