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
    public boolean add(T value) {
        Node<T> newNode = new Node<>();
        newNode.key = value;
        while (true) {
            Neighbours<T> neighbours = searchNeighbours(new KeyHolder<>(value));
            if (neighbours.right != tail && neighbours.right.key.compareTo(value) == 0) {
                return false;
            }

            newNode.nextRef = new AtomicMarkableReference<>(neighbours.right, false);

            if (neighbours.left.nextRef.compareAndSet(neighbours.right, newNode, false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(T value) {
        NeighboursCandidates<T> neighborCandidates = findNeighboursCandidates(new KeyHolder<>(value));
        return neighborCandidates.right != tail && neighborCandidates.right.key.compareTo(value) == 0;
    }

    @Override
    public boolean isEmpty() {
        Neighbours<T> neighbours = searchNeighbours(null);
        return neighbours.left == head && neighbours.right == tail;
    }

    private Neighbours<T> searchNeighbours(KeyHolder<T> keyHolder) {
        while (true) {
            NeighboursCandidates<T> neighborCandidates = findNeighboursCandidates(keyHolder);
            Node<T> left = neighborCandidates.left;
            Node<T> leftNext = neighborCandidates.leftNext;
            Node<T> right = neighborCandidates.right;

            boolean adjacent = leftNext == right && !right.isDeleted();
            if (adjacent) {
                return new Neighbours<>(left, right);
            }

            boolean removedMarkedNodes = leftNext != right && left.nextRef.compareAndSet(leftNext, right, false, false);
            if (removedMarkedNodes && !right.isDeleted()) {
                return new Neighbours<>(left, right);
            }
        }
    }

    // rightCandidate - это первая нода с node.key >= key
    private NeighboursCandidates<T> findNeighboursCandidates(KeyHolder<T> keyHolder) {
        NeighboursCandidates<T> result = new NeighboursCandidates<>();
        Node<T> curNode = head;
        boolean[] curNodeNextMarked = {false};
        Node<T> curNodeNext = curNode.nextRef.get(curNodeNextMarked);

        do {
            if (!curNodeNextMarked[0]) { //head.next will never be marked
                result.left = curNode;
                result.leftNext = curNodeNext;
            }

            curNode = curNodeNext;
            if (curNode == tail) {
                break;
            }
            curNodeNext = curNode.nextRef.get(curNodeNextMarked);
        } while (curNodeNextMarked[0] || keyHolder == null || curNode.key.compareTo(keyHolder.key) < 0);
        result.right = curNode;
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
        final Node<T> left;
        final Node<T> right;

        Neighbours(Node<T> left, Node<T> right) {
            this.left = left;
            this.right = right;
        }
    }

    private static class NeighboursCandidates<T> {
        Node<T> left;
        Node<T> leftNext;
        Node<T> right;
    }

    private static class KeyHolder<T> {
        T key;

        KeyHolder(T key) {
            this.key = key;
        }
    }
}
