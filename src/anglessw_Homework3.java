import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CSE381 Homework 3
 * @author Steven Angles
 *
 */
public class anglessw_Homework3 extends Thread {
	/**
	 * The number of command line arguments that this program accepts.
	 * 
	 * The first is the path to the directory to search.
	 * The second is the substring that is present in all files to be processed.
	 * The third is the number of threads to use when processing.
	 */
	private static final int NUM_ARGS = 3;
	
	/**
	 * ISO encoding string to use for the scanner so that the Scanner
	 * correctly interprets files on Linux machines.
	 */
	private static final String ISO_ENCODING = "ISO-8859-1";

	/**
	 * A string to print if the command line arguments are incorrect.
	 */
	private static final String INSUFFICIENT_ARGS = "Error: insufficient number of arguments.";
	
	/**
	 * The string to print to notify the user that files are being processed.
	 */
	private static final String FILES_PROCESSED = "Files to be processed:";
	
	/**
	 * The global queue that all files are added to as the directory is explored.
	 */
	private static final ArrayList<File> queue = new ArrayList<File>();
	
	/**
	 * The global counter for the items removed from the queue.
	 */
	private static final AtomicInteger counter = new AtomicInteger(0);
	
	/**
	 * The global counter for the items that have been printed.
	 */
	private static final AtomicInteger printedCounter = new AtomicInteger(0);
	
	/**
	 * The global queue where threads place processed items.
	 * 
	 * Items are prioritized by index (lowest first) according to the comparator passed in as
	 * an argument.
	 */
	private static final PriorityQueue<String> processedQueue = new PriorityQueue<String>(5, new Comparator<String>() {
		public int compare(String s1, String s2) {
			int s1Idx = getIndex(s1);
			int s2Idx = getIndex(s2);
			
			return (s1Idx - s2Idx);
		}
	});
	
	/**
	 * The HashSet that contains the english words from english.txt.
	 */
	private static final HashSet<String> dictionary = new HashSet<String>();
	
	/**
	 * The text file name for the supplied english dictionary.
	 */
	private static final String DICTIONARY_TEXT = "english.txt";

	/**
	 * Run method for each thread. Called with Thread.start().
	 * 
	 * In this case, the threads act as both producers and consumers.
	 */
	public void run() {
		try {
			produceAndConsume();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The producer and consumer dual-function method.
	 * 
	 * Each thread processes files and places them on the processed queue (producing)
	 * while also removing any files that are ready from the processed queue and printing them (consuming).
	 * @throws IOException 
	 * 					if a problem is encountered with file I/O
	 * @throws InterruptedException 
	 * 					if a problem is encountered with thread waiting.
	 */
	private void produceAndConsume() throws IOException, InterruptedException {
		while (queue.size() > 0) {
			int count;
			File f = null;
			synchronized (queue) {
				f = queue.remove(0);
				count = counter.getAndIncrement();
				queue.notifyAll();
			} // synchronized queue
			
			String s = processFile(f, count);		
			synchronized (processedQueue) {
				processedQueue.add(s);
				while (processedQueue.isEmpty()) {
					processedQueue.wait();
				}
				while (!processedQueue.isEmpty() && 
						printedCounter.get() == getIndex(processedQueue.peek())) {
					System.out.println(processedQueue.remove());
					printedCounter.incrementAndGet();
				}
				processedQueue.notifyAll();
			} // synchronized globalQueue
		}
	}
	
	/**
	 * Gets the index of a string with the schema:
	 * 				index: file path
	 * 						Process info here
	 * For example, passing in the string:
	 * 				0: C:\default.txt
	 * 						Lines: 1, words: 3, english words: 4
	 * to getIndex() will return an int of 0.
	 * @param s The string to process
	 * @return The index that was found.
	 */
	private static int getIndex(String s) {
		int i = 0;
		String idx = "";
		while (Character.isDigit(s.charAt(i))) {
			idx += s.charAt(i);
			i++;
		}
		return Integer.parseInt(idx);
	}
	
	/**
	 * Generates the dictionary by reading in the words from english.txt.
	 * 
	 * Ignores any words with non-alphabetic letters or uppercase present.
	 * @throws IOException
	 * 					If there is a problem reading the file.
	 */
	private static void generateDictionary() throws IOException {
		Scanner fileReader = new Scanner(new File(DICTIONARY_TEXT), ISO_ENCODING);

		while (fileReader.hasNextLine()) {
			String word = fileReader.nextLine();
			
			boolean clean = true;
			for (int i=0; i<word.length(); i++) {
				char cur = word.charAt(i);
				if (!Character.isAlphabetic(cur) || 
						Character.isUpperCase(cur)) {
					clean = false;
				}
			}
			
			if (clean) {
				dictionary.add(word);
			}
		}
		
		fileReader.close();
	}
	
	/**
	 * Kicks off the threads that process the files and waits for them to finish.
	 * 
	 * @param numThreads
	 * 				The number of threads to use to process.
	 * @throws InterruptedException
	 * 				If there is a problem joining the threads.
	 */
	private static void processFiles(int numThreads) throws InterruptedException {
		System.out.println("** Processing " + queue.size() + " files using "
				+ numThreads + " threads:");
		anglessw_Homework3[] threads = new anglessw_Homework3[numThreads];
		for (int i=0; (i < threads.length); i++) { 
			threads[i] = new anglessw_Homework3();
			threads[i].start();
		}
		
		for (anglessw_Homework3 thread : threads) {
			thread.join();
		}
	}
	
	/**
	 * Processes a file for lines, words and english words.
	 * 
	 * @param f The file to process.
	 * @param idx The index of the file in the queue.
	 * @return The file index with path and process info in this form:
	 * 				idx: file path
	 * 					file info
	 * @throws IOException
	 * 			If there is a problem reading input from the file.
	 */
	private static String processFile(File f, int idx) throws IOException {
		int lineCount = 0;
		int wordCount = 0;
		int englishWordCount = 0;
		
		Scanner fileReader = new Scanner(f, ISO_ENCODING);
		
		while (fileReader.hasNextLine()) {
			lineCount++;
			String line = fileReader.nextLine();
			line = line.toLowerCase();
			
			// create array of "words"
			String[] words = line.split("\\s+");
			wordCount += numWords(words);
			englishWordCount += numEnglishWords(words);
		}
		
		fileReader.close();
		
		String path = idx+": "+f.getAbsolutePath();
		return path+"\n\tLines: "+lineCount+", words: "+wordCount+", english words: "+englishWordCount;
	}
	
	/**
	 * Returns the number of words found in a string array.
	 * 
	 * Words are defined as any non-whitespace.
	 * 
	 * @param words The string array to process.
	 * @return The number of words found in the array.
	 */
	private static int numWords(String[] words) {
		int count = 0;
		for (String s : words) {
			if (!s.trim().isEmpty()) count++;
		}
		
		return count;
	}
	
	/**
	 * Returns the number of english words found in a string array.
	 * 
	 * English words are defined as any non-whitespace that is present
	 * in the dictionary HashMap generated from the supplied english.txt file.
	 * 
	 * @param words The string array containing the words to be processed.
	 * @return The number of english words found in the array.
	 */
	private static int numEnglishWords(String[] words) {
		int count = 0;
		for (String s : words) {
			if (!s.trim().isEmpty()) {
				if (dictionary.contains(s)) count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Prints the files found by recursively crawling the directory
	 * and all subdirectories so long as the file matches the specified
	 * substring.
	 * 
	 * @param entry The directory to search for files.
	 * @param subString The substring to search for in the file names.
	 * @throws IOException
	 * 				If there is a problem accessing the file.
	 */
	private static void listFiles(File entry, String subString) throws IOException {
		// search the directory recursively for
		// files and add them to the queue
		traverseDirectory(entry, subString);
		System.out.println(FILES_PROCESSED);
		for (int i=0; i<queue.size(); i++) {
			System.out.println(i+": " + queue.get(i).getAbsolutePath());
		}
		System.out.print("\n");
	}
	
	/**
	 * Recursive crawler that looks for files in a DFS.
	 * 
	 * @param entry The file/directory to crawl.
	 * @param subString THe substring to match in the file name.
	 * @throws IOException
	 * 				If there is a problem accessing the file.
	 */
	private static void traverseDirectory(File entry, String subString) throws IOException {
		if (entry.isDirectory() &&
				entry.getCanonicalPath().equals(entry.getAbsolutePath())) {
			for (File f : entry.listFiles()) {
				traverseDirectory(f, subString);
			}
		} else if (entry.canRead() && entry.isFile() &&
				entry.getName().contains(subString)) {
			queue.add(entry);
			return;
		}
	}
	
	/**
	 * Main class that gets the ball rolling.
	 * 
	 * @param args The string array containing the command line arguments.
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != NUM_ARGS) {
			System.err.println(INSUFFICIENT_ARGS);
			return;
		}
		
		generateDictionary();
		
		// the subdirectory to be recursively searched
		File entry = new File(args[0]);
		
		// the substring to match in each file
		String subString = args[1];
		
		// the number of cores to use
		int numThreads = Integer.parseInt(args[2]);
		
		// list the files to be processed (always single threaded)
		listFiles(entry, subString);
		// process files with the given number of threads
		processFiles(numThreads);
	}
}
