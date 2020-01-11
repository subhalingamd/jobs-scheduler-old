package ProjectManagement;

import PriorityQueue.PriorityQueueDriverCode;

import java.io.*;
import java.net.URL;


//MY IMPORTS
import PriorityQueue.*;
import RedBlack.*;
import Trie.*;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Iterator;

public class Scheduler_Driver extends Thread implements SchedulerInterface {
    private Trie<Project> allProjects = new Trie();
    private MaxHeap<Job> allJobs = new MaxHeap();
    private Trie<Job> jobList = new Trie();
    private RBTree<User,User> allUsers = new RBTree();

    private RBTree<String,Job> expensiveJobsUpd = new RBTree();

    private Queue<Job> completedJobs=new LinkedList();
    private Queue<Job> expensiveJobs=new LinkedList();

    private int pendingJob=0;
    private int idGenerator=0;


    public static void main(String[] args) throws IOException {
        Scheduler_Driver scheduler_driver = new Scheduler_Driver();

        File file;
        if (args.length == 0) {
            URL url = PriorityQueueDriverCode.class.getResource("INP");
            file = new File(url.getPath());
        } else {
            file = new File(args[0]);
        }

        scheduler_driver.execute(file);
    }

    public void execute(File file) throws IOException {

        URL url = Scheduler_Driver.class.getResource("INP");
        file = new File(url.getPath());

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.err.println("Input file Not found. "+file.getAbsolutePath());
        }
        String st;
        while ((st = br.readLine()) != null) {
            String[] cmd = st.split(" ");
            if (cmd.length == 0) {
                System.err.println("Error parsing: " + st);
                return;
            }

            switch (cmd[0]) {
                case "PROJECT":
                    handle_project(cmd);
                    break;
                case "JOB":
                    handle_job(cmd);
                    break;
                case "USER":
                    handle_user(cmd[1]);
                    break;
                case "QUERY":
                    handle_query(cmd[1]);
                    break;
                case "":
                    handle_empty_line();
                    break;
                case "ADD":
                    handle_add(cmd);
                    break;
                default:
                    System.err.println("Unknown command: " + cmd[0]);
            }
        }


        run_to_completion();

        print_stats();

    }




    @Override
    public void run() {
        // till there are JOBS
        schedule();
    }


    @Override
    public void run_to_completion() {
        while (!allJobs.isEmpty()){
            schedule();
            System.out.println("System execution completed");
        }

    }

    @Override
    public void handle_project(String[] cmd) {
        System.out.println("Creating project");
        Project newProj= new Project(cmd[1],Integer.parseInt(cmd[2]),Integer.parseInt(cmd[3]));
        allProjects.insert(cmd[1],newProj);
    }

    @Override
    public void handle_job(String[] cmd) {
        System.out.println("Creating job");
        TrieNode<Project> getProj=(TrieNode)allProjects.search(cmd[2]);
        if (getProj==null){
            System.out.println("No such project exists. "+cmd[2]);
            return;
        }
        RedBlackNode<User,User> getUser=allUsers.search(new User(cmd[3]));
        if (getUser.getValues()==null){
            System.out.println("No such user exists: "+cmd[3]);
            return;
        }
        Job newJob=new Job(cmd[1],getProj.getValue(),getUser.getValue(),Integer.parseInt(cmd[4]),idGenerator++);
        allJobs.insert(newJob);
        jobList.insert(cmd[1],newJob);
        pendingJob++;
    }

    @Override
    public void handle_user(String name) {
        System.out.println("Creating user");
        allUsers.insert(new User(name),new User(name));
    }

    @Override
    public void handle_query(String key) {
        System.out.println("Querying");
        System.out.print(key+": ");
        TrieNode<Job> currJobNode = (TrieNode<Job>)jobList.search(key);
        if (currJobNode==null){
            System.out.println("NO SUCH JOB");
            return;
        }

        Job currJob = currJobNode.getValue();
        Iterator<Job> iter=completedJobs.iterator();
        while (iter.hasNext()){
            if (iter.next()==currJob){
                System.out.println("COMPLETED");
                return;
            }
        }
        System.out.println("NOT FINISHED");

    }

    @Override
    public void handle_empty_line() {
        schedule();
        System.out.println("Execution cycle completed");
    }

    @Override
    public void handle_add(String[] cmd) {
        System.out.println("ADDING Budget");
        TrieNode<Project> getProj=(TrieNode)allProjects.search(cmd[1]);
        if (getProj==null){
            System.out.println("No such project exists. "+cmd[1]);
            return;
        }
        getProj.getValue().addBudget(Integer.parseInt(cmd[2]));

        MaxHeap<Job> temp = new MaxHeap();
        Queue<Job> temp2=new LinkedList();
        while (!expensiveJobs.isEmpty()){
            Job expensiveJob=expensiveJobs.remove();
            if (expensiveJob.getProject()==getProj.getValue()){
                temp.insert(expensiveJob);
                pendingJob++;
            }
            else 
                temp2.add(expensiveJob);
        }
        expensiveJobs=temp2;

        //$$$$ changed here
        while (!allJobs.isEmpty()){
            //Job t=allJobs.extractMax();
            //if (t==null)
            //    break;
            temp.insert(allJobs.extractMax());
        }
        allJobs=temp;
        //Addback expensive jobs
    }

    @Override
    public void print_stats() {
        System.out.println("--------------STATS---------------");

        int count=completedJobs.size();
        System.out.println("Total jobs done: "+count);

        Job curr;
        int endtime=0;
        while (!completedJobs.isEmpty()){
            curr=completedJobs.remove();
            endtime+=curr.getRuntime();
            System.out.println("Job{user=\'"+curr.getUser()+"\', project=\'"+curr.getProject()+"\', jobstatus=COMPLETED, execution_time="+curr.getRuntime()+", end_time="+endtime+", name=\'"+curr+"\'}");
        }
        System.out.println("------------------------");


        count=expensiveJobs.size();
        System.out.println("Unfinished jobs: ");

        while (!expensiveJobs.isEmpty()){
            curr=expensiveJobs.remove();
            System.out.println("Job{user=\'"+curr.getUser()+"\', project=\'"+curr.getProject()+"\', jobstatus=REQUESTED, execution_time="+curr.getRuntime()+", end_time=null, name=\'"+curr+"\'}");
        }
        System.out.println("Total unfinished jobs: "+count);
        System.out.println("--------------STATS DONE---------------");
    }

    @Override
    public void schedule() {
        System.out.println("Running code");
        System.out.println("Remaining jobs: "+pendingJob);
        while (true){
            Job currJob=allJobs.extractMax();
            if (currJob==null){
                return;
            }
            System.out.println("Executing: "+currJob+" from: "+currJob.getProject());
            Project jobIn = currJob.getProject();
            pendingJob--;
            if (jobIn.getBudget()>=currJob.getRuntime()){
                completedJobs.add(currJob);
                jobIn.addBudget(0-currJob.getRuntime());
                System.out.println("Project: "+jobIn+" budget remaining: "+jobIn.getBudget());
                return;
            }
            System.out.println("Un-sufficient budget.");
            expensiveJobsUpd.insert(jobIn.toString(),currJob);
            expensiveJobs.add(currJob);
        }

    }
}
