package site.ilemon.list;

/**
 * <p>DoublyLinkedList</p>  
 * <p>Doubly linked list storing any data </p>  
 * @author yanan  
 * @date 2018-03-15
 */
public class DoublyLinkedList<T> {
    private DNode<T> head,tail;
	private int size;
	public DoublyLinkedList(){
		head=tail=new DNode<T>(null,null, null);
	}
	
	public void clear(){
		head = tail = new DNode<T>(null,null, null);
		size = 0;
	}
	
	public int size(){
		return size;
	}
	
	public boolean isEmpty(){
		return size==0?true:false;
	}
	/**
	 * Add data to the head of the linked list
	 * @param data Data to be added
	 */
	public void addToHead(T data){
		if(isEmpty()){
			head=tail=new DNode<T>(data, null, null);
		}else{
			DNode<T> newNode = new DNode<T>(data, null, null);
			newNode.next=head;
			head.prev=newNode;
			head=newNode;
		}
		size++;
	}
	
	/**
	 * Add data to the tail of the linked list
	 * @param data Data to be added
	 */
	public void addToTail(T data){
		if(isEmpty()){
			head=tail=new DNode<T>(data, null, null);
		}else{
			DNode<T> newNode = new DNode<T>(data, null, null);
			tail.next=newNode;
			newNode.prev=tail;
			tail=tail.next;
		}
		size++;
	}
	
	/**
	 * Append all elements of another linked list to the tail of this linked list
	 * @param list Linked list to be appended
	 */
	public void addAll(DoublyLinkedList<T> list){
		if (list == null || list.isEmpty()) return;
		for(int i = 0; i < list.size(); i++) {
			this.addToTail(list.get(i));
		}
	}

	/**
	 * Delete data from the head of the linked list and return it. If the list is empty, return null.
	 */
	public T deleteFromHead(){
		if(!isEmpty()){
			T value=head.info;
			if(size==1){
				head=tail=new DNode<T>(null, null, null);
			}else{
				head=head.next;
				head.prev=null;
			}
			size--;
			return value;
		}
		return null;
	}
	
	/**
	 * Add data to the middle of the linked list (Exercise 16 of 3.10)
	 * @param data Data to be added
	 */
	public void addToMiddle(T data){
		DNode<T> newNode = new DNode<T>(data, null, null);
		if(isEmpty()){
			head=tail=newNode;
		}else{
			int index=(size()>>1)-1;
			DNode<T> temp=head;
			while(index>0){
				temp=temp.next;
				index--;
			}
			newNode.next=temp.next;
			temp.next.prev=newNode;
			temp.next=newNode;
			newNode.prev=temp;
		}
		size++;
	}
	
	/**
	 * Delete data from the tail of the linked list and return it. If the list is empty, return null.
	 */
	public T deleteFromTail(){
		if(!isEmpty()){
			T value=tail.info;
			if(size==1){
				head=tail=new DNode<T>(null, null, null);
			}else{
				tail=tail.prev;
				tail.next=null;
			}
			size--;
			return value;
		}
		return null;
	}
	
	/**
	 * Delete data at the specified position from the linked list
	 * @param index Index of the data in the linked list
	 */
	public void deleteNode(int index){
		rangeCheck(index);
		if(index==0){
			deleteFromHead();
		}else if(index==size-1){
			deleteFromTail();
		}else{
			int i=0;
			DNode<T> tmpNode=head;
			while(i<index){
				tmpNode=tmpNode.next;
				i++;
			}
			tmpNode.prev.next=tmpNode.next;
			tmpNode.next.prev=tmpNode.prev;
			size--;
		}
		
	}
	
	
	/**
	 * Get data at the specified position in the linked list
	 * @param i Index
	 * @return int Data
	 */
	public T get(int i){
		rangeCheck(i);
		if(i==0){
			return head.info;
		}
		if(i==size-1){
			return tail.info;
		}
		int tmpIndex=0;
		DNode<T> tmpNode=head; 
		while(tmpIndex!=i){
			tmpNode=tmpNode.next;
			tmpIndex++;
		}
		return tmpNode.info;
	}
	
	private void rangeCheck(int i) {
		if(i<0 || i>=size){
			throw new ArrayIndexOutOfBoundsException(i);
		}
	}
}

class DNode<T>{
	T info;
	DNode<T> prev,next;
	public DNode(T info, DNode<T> prev, DNode<T> next) {
		super();
		this.info = info;
		this.prev = prev;
		this.next = next;
	}
	
}