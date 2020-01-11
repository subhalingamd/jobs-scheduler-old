package ProjectManagement;


public class Project {
	String name;
	int priority,budget,time;

	public Project(String name,int priority,int budget){
		this.name=name;
		this.budget=budget;
		this.priority=priority;
	}

	public void addBudget(int append){
		budget+=append;
	}

	public int getBudget(){
		return budget;
	}

	public int getPriority(){
		return priority;
	}

	@Override
	public String toString(){
		return name;
	}
}
