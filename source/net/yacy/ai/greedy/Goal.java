/**
 *  Goal.java
 *  Copyright 2009 by Michael Peter Christen, Frankfurt a. M., Germany
 *  First published 03.12.2009 at http://yacy.net
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file COPYING.LESSER.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.ai.greedy;


public interface Goal<
                        SpecificRole extends Role,
                        SpecificFinding extends Finding<SpecificRole>,
                        SpecificModel extends Model<SpecificRole, SpecificFinding>
                       > {

    public boolean pruning(SpecificModel model);
    public boolean isSnapshot(SpecificModel model);
    public boolean isFulfilled(SpecificModel model);
}
