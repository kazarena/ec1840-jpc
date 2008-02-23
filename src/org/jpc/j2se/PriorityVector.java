/*
    JPC: A x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.0

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007 Isis Innovation Limited

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 
    Details (including contact information) can be found at: 

    www.physics.ox.ac.uk/jpc
*/

package org.jpc.j2se;

import java.util.*;
import org.jpc.emulator.*;

public class PriorityVector
{
    private Vector backingVector;

    public PriorityVector(int initialSize)
    {
	backingVector = new Vector(initialSize);
    }

    public void addComparableObject(ComparableObject obj)
    {
	//Can only add elements through here, so we can optimise more easily
	synchronized (backingVector) {
	    for (int i=0; i<size()-1; i++) {
		ComparableObject t = (ComparableObject) backingVector.elementAt(i);
		if (obj.compareTo(t) <= 0) {
		    backingVector.insertElementAt(obj, i);
		    return;
		}
	    }
	    backingVector.addElement(obj); //adds to end of list
	}
    }

    public int size()
    {
	return backingVector.size();
    }

    public Object firstElement()
    {
	try {
	    return backingVector.firstElement();
	} catch (NoSuchElementException e) {
	    return null;
	}
    }

    public void printContents()
    {
        for (int i=0; i< backingVector.size(); i++)
        {
            System.out.println(backingVector.get(i).getClass().getName());
        }
    }

    public void removeFirstElement()
    {
	backingVector.removeElementAt(0);
    }

    public void removeIfFirstElement(Object first)
    {
	synchronized (backingVector) {
	    if (backingVector.elementAt(0) == first)
		backingVector.removeElementAt(0);
	}
    }

    public void removeElement(Object element)
    {
	backingVector.removeElement(element);
    }
}
