package sa.test.testsrc;

public class Src {

	public void doWork0() {
		int N = 11;
		int M = 12;
		int L = 13;
		int i = 0;
	
		for (i=0; i<N; i++) {
			int a = 0;
			int b = 1;
			int c = a + b;
			if (i>M)
				break;
		}
		
		int x = N+M;
		i = 0;
		while (i<N) {
			int a = 0;
			int b = 1;
			int c = a + b;
			if (i>M)
				break;
			i++;
		}
		
		i = 0;
		while (i<N || i<L) {
			int a = 0;
			int b = 1;
			int c = a + b;
			if (i>M)
				break;
			i++;
		}
		
	}
	
	
	public void doWork1() {
		int N = 10;
		int M = 20;
		int L = 30;
		int i = 0;
		
		while (i<N && i<M && i<L) {
			int a = 0;
			int b = 1;
			int c = a + b;
			i++;
		}
	}
	
	
	public void doWork2() {
		int N = 10;
		int M = 20;
		int L = 30;
		int i = 0;
		
		while (i<N || i<M || i<L) {
			int a = 0;
			int b = 1;
			int c = a + b;
			i++;
		}
	}
	
}
