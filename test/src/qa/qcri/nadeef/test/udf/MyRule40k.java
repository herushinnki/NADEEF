package qa.qcri.nadeef.test.udf;/*
 * Copyright (C) Qatar Computing Research Institute, 2013.
 * All rights reserved.
 */
import qa.qcri.nadeef.core.datamodel.*;
import java.util.*;


public class MyRule40k extends PairTupleRule {
    protected List<Column> leftHandSide = new ArrayList();
    protected List<Column> rightHandSide = new ArrayList();

    public MyRule40k() {}

    @Override
    public void initialize(String id, List<String> tableNames) {
        super.initialize(id, tableNames);
        leftHandSide.add(new Column("csvtable_hospital_40k.zipcode"));

        rightHandSide.add(new Column("csvtable_hospital_40k.city"));

    }

    /**
     * Default horizontal scope operation.
     * @param tupleCollections input tuple collections.
     * @return filtered tuple collection.
     */
    @Override
    public Collection<TupleCollection> horizontalScope(
        Collection<TupleCollection> tupleCollections
    ) {
        tupleCollections.iterator().next().project(leftHandSide).project(rightHandSide);
        return tupleCollections;
    }

    /**
     * Default block operation.
     * @param tupleCollections a collection of tables.
     * @return a collection of blocked tables.
     */
    @Override
    public Collection<TupleCollection> block(Collection<TupleCollection> tupleCollections) {
        TupleCollection tupleCollection = tupleCollections.iterator().next();
        Collection<TupleCollection> groupResult = tupleCollection.groupOn(leftHandSide);
        return groupResult;
    }

    /**
     * Default group operation.
     *
     * @param tuples input tuple
     * @return a group of tuple collection.
     */
    @Override
    public Collection<TuplePair> iterator(TupleCollection tuples) {
        ArrayList<TuplePair> result = new ArrayList();
        tuples.orderBy(rightHandSide);
        int pos1 = 0, pos2 = 0;
        boolean findViolation = false;

        // ---------------------------------------------------
        // two pointer loop via the block. Linear scan
        // ---------------------------------------------------
        while (pos1 < tuples.size()) {
            findViolation = false;
            for (pos2 = pos1 + 1; pos2 < tuples.size(); pos2 ++) {
                Tuple left = tuples.get(pos1);
                Tuple right = tuples.get(pos2);
                for (Column column : rightHandSide) {
                    Object lvalue = left.get(column);
                    Object rvalue = right.get(column);
                    if (
                        (lvalue == null && rvalue != null) ||
                            (lvalue != null && !lvalue.equals(rvalue))
                        ) {
                        findViolation = true;
                        break;
                    }
                }

                // generates all the violations between pos1 - pos2.
                if (findViolation) {
                    for (int i = pos1; i < pos2; i ++) {
                        for (int j = pos2; j < tuples.size(); j++) {
                            TuplePair pair = new TuplePair(tuples.get(i), tuples.get(j));
                            result.add(pair);
                        }
                    }
                    break;
                }
            }
            pos1 = pos2;
        }

        return result;
    }

    /**
     * Detect method.
     * @param tuplePair tuple pair.
     * @return violation set.
     */
    @Override
    public Collection<Violation> detect(TuplePair tuplePair) {
        Tuple left = tuplePair.getLeft();
        Tuple right = tuplePair.getRight();
        List<Violation> result = new ArrayList();
        for (Column column : rightHandSide) {
            Object lvalue = left.get(column);
            Object rvalue = right.get(column);
            if (!lvalue.equals(rvalue)) {
                Violation violation = new Violation(id);
                violation.addTuple(left);
                violation.addTuple(right);
                result.add(violation);
                break;
            }
        }
        return result;
    }

    /**
     * Repair of this rule.
     *
     * @param violation violation input.
     * @return a candidate fix.
     */
    @Override
    public Collection<Fix> repair(Violation violation) {
        List<Fix> result = new ArrayList();
        Collection<Cell> cells = violation.getCells();
        HashMap<Column, Cell> candidates = new HashMap<Column, Cell>();
        int vid = violation.getVid();
        Fix fix;
        Fix.Builder builder = new Fix.Builder(violation);
        for (Cell cell : cells) {
            Column column = cell.getColumn();
            if (rightHandSide.contains(column)) {
                if (candidates.containsKey(column)) {
                    // if the right hand is already found out in another tuple
                    Cell right = candidates.get(column);
                    fix = builder.left(cell).right(right).build();
                    result.add(fix);
                } else {
                    // it is the first time of this cell shown up, put it in the
                    // candidate and wait for the next one shown up.
                    candidates.put(column, cell);
                }
            }
        }
        return result;
    }
}