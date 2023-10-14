import javafx.util.Pair;

import java.util.*;


class ListUtil {

    int size;
    Node head, tail;

    class Node {
        Integer value;
        Node prev, next;

        public Node(Integer x) {
            this.value = x;
        }

        public Node() {

        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Node getPrev() {
            return prev;
        }

        public void setPrev(Node prev) {
            this.prev = prev;
        }

        public Node getNext() {
            return next;
        }

        public void setNext(Node next) {
            this.next = next;
        }
    }

    {
        head = new Node();
        tail = new Node();
        head.next = tail;
        tail.prev = head;
    }

    /**
     * 向双向链表指定一个位置插入一个节点
     * @param x 表示插入的节点其中的值 value
     * @param index 表示插入的位置，从0开始
     * @return 是否插入成功
     */
    boolean addToIndex(Integer x, int index) {
        if(index < 0 || index > size) return false;

        Node cur = head;
        Node newNode = new Node(x);
        for(int i = 0; i < index - 1; i ++) {
            if(cur == null) break;
            cur = cur.next;
        }

        Node nxt = cur.next;

        cur.next = newNode;
        newNode.prev = cur;
        newNode.next = nxt;
        nxt.prev = newNode;

        size ++ ;

        return true;
    }

    public void printList() {
        Node cur = head.next;
        while(cur != null && cur != tail) {
            System.out.print(cur.getValue() + " ");
            cur = cur.next;
        }
    }

}

public class CSDN_COM {

    public static void main(String[] args) {

        ListUtil util = new ListUtil();

        // boolean res = util.addToIndex(100, 0);
        boolean res = util.addToIndex(100, 6);

        System.out.println(res);

        util.printList();


        /**
         *
         * id name score
         *
         *
         * t1
         *
         * select id, name, score from table where name = '张三';
         *
         * t2
         *
         * score > 60
         *
         * tip: 重名
         * tip: 一个学生可能有多条记录
         *
         * id 就是标识一个学生的话：
         * select id, name from table where score > 60 group by id, name
         *
         *
         * name = '张三' score > 60
         *
         *
         *
         */



    }
}