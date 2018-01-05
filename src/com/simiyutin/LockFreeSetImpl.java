package com.simiyutin;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class LockFreeSetImpl<T extends Comparable<T>> implements LockFreeSet<T> {
    private final Node<T> head;
    private final Node<T> tail;

    public LockFreeSetImpl() {
        head = new Node<>();
        tail = new Node<>();
        head.nextRef = new AtomicMarkableReference<>(tail, false);
    }

    @Override
    public boolean add(T key) {
        Node<T> newNode = new Node<>();
        newNode.key = key;
        while (true) {
            Neighbours<T> neighbours = search(key);
            if (neighbours.rightNeighbour != tail && neighbours.rightNeighbour.key.compareTo(key) == 0) {
                return false;
            }

            newNode.nextRef = new AtomicMarkableReference<>(neighbours.rightNeighbour, false);

            if (neighbours.leftNeighbour.nextRef.compareAndSet(neighbours.rightNeighbour, newNode, false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(T value) {
        return false;
    }

    @Override
    public boolean contains(T value) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    private Neighbours<T> search(T key) {
        while (true) {
            NeighboursCandidates<T> candidates = findCandidatesNeighbours(key);
            Node<T> left = candidates.leftNeighbourCandidate;
            Node<T> leftNext = candidates.leftNeighbourCandidateNext;
            Node<T> right = candidates.rightNeighbourCandidate;

            boolean adjacent = leftNext == right && !right.isDeleted();
            if (adjacent) {
                return new Neighbours<>(left, right);
            }

            boolean removedDeletedNodes = leftNext != right && left.nextRef.compareAndSet(leftNext, right, false, false);
            if (removedDeletedNodes && !right.isDeleted()) {
                return new Neighbours<>(left, right);
            }
        }
    }

    // rightCandidate - это первая нода с node.key >= key
    private NeighboursCandidates<T> findCandidatesNeighbours(T key) {
        NeighboursCandidates<T> result = new NeighboursCandidates<>();
        Node<T> curNode = head;
        boolean[] curNodeNextMarked = {false};
        Node<T> curNodeNext = curNode.nextRef.get(curNodeNextMarked);

        do {
            if (!curNodeNextMarked[0]) { //head.next will never be marked
                result.leftNeighbourCandidate = curNode;
                result.leftNeighbourCandidateNext = curNodeNext;
            }

            curNode = curNodeNext;
            if (curNode == tail) {
                break;
            }
            curNodeNext = curNode.nextRef.get(curNodeNextMarked);
        } while (curNodeNextMarked[0] || curNode.key.compareTo(key) < 0);
        result.rightNeighbourCandidate = curNode;
        return result;
    }


    private static class Node<T> {
        T key;
        AtomicMarkableReference<Node<T>> nextRef;

        boolean isDeleted() {
            return nextRef != null && nextRef.isMarked();
        }
    }

    private static class Neighbours<T> {
        Node<T> leftNeighbour;
        Node<T> rightNeighbour;

        public Neighbours(Node<T> leftNeighbour, Node<T> rightNeighbour) {
            this.leftNeighbour = leftNeighbour;
            this.rightNeighbour = rightNeighbour;
        }
    }

    private static class NeighboursCandidates<T> {
        Node<T> leftNeighbourCandidate;
        Node<T> leftNeighbourCandidateNext;
        Node<T> rightNeighbourCandidate;
    }
}
