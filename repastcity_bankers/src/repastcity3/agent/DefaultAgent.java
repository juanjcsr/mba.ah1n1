/*
©Copyright 2012 Nick Malleson
This file is part of RepastCity.

RepastCity is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

RepastCity is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with RepastCity.  If not, see <http://www.gnu.org/licenses/>.
 */

package repastcity3.agent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import repast.simphony.query.space.gis.GeographyWithin;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repastcity3.environment.Building;
import repastcity3.environment.Route;
import repastcity3.main.ContextManager;
import repastcity3.main.GlobalVars;

public class DefaultAgent implements IAgent {

	private static Logger LOGGER = Logger.getLogger(DefaultAgent.class.getName());
	private String name;

	private Building home; // Where the agent lives
	private Building workplace; // Where the agent works
	private ArrayList<Building> funplaces;
	private Route route; // An object to move the agent around the world
	private Building currentBuilding;
	
	private double workingTimeStart;
	private double workingTimeEnd;
	private double funTimeStart;

	private boolean goingHome = false; // Whether the agent is going to or from their home

	private static int uniqueID = 0;
	private int id;
	
	private AgentType type;
	private AgentStatus status;
	private ArrayList<DefaultAgent> infectedList;
	
	
	private static int susceptibleCounter = 0;
	private static int exposedCounter = 0;
	private static int recoveredCounter = 0;

	private static int infectedCounter;
	
	/**
	 * Referencia al agente que me infecto
	 */
	private DefaultAgent whoInfectedMe;
	
	private int daysExposed = 0;
	
	/**
	 * Contador que lleva los dias infectados de un agente
	 */
	private int daysInfected = 0;
	
	/**
	 * Periodo de incubacion (agente expuesto) del virus.
	 */
	private int incubationPeriod;
	/**
	 * Periodo de duracion de sintomas
	 */
	private int symptomsPeriod;
	
	/***
	 * 
	 */
	public static boolean isContingencyOn = false;

	public DefaultAgent() {
		this.id = uniqueID++;
		this.infectedList = new ArrayList<DefaultAgent>();
		//Get the random occupation of the Agent
		int type = ContextManager.getUniformRandom(99);
		this.status = AgentStatus.SUSCEPTIBLE;
		funplaces = new ArrayList<Building>();
		//50% of the population will be workers
		if (type < GlobalVars.InfluenzaModelParams.WORKERS) {
			
			this.type = AgentType.WORKER;
			// Find a building that agents can use as their workplace. First, iterate over all buildings in the model
	        for (Building b : ContextManager.buildingContext.getRandomObjects(Building.class, 10000)) {
	                // See if the building is a bank (they have type==2).
	                if (b.getType()==2) {
	                        this.workplace = b;
	                        break; // Have found a bank, stop searching.
	                }
	        }
	        
		}
		//Students
		else if ( type >= GlobalVars.InfluenzaModelParams.WORKERS && type < GlobalVars.InfluenzaModelParams.STUDENTS ){

			this.type = AgentType.STUDENT;
			Random rndBuildings = new Random();
			Building b = ContextManager.type3.get(rndBuildings.nextInt(ContextManager.type3.size()));
			this.workplace = b;		
			
		}
		//Otros agentes
		else if ( type >= GlobalVars.InfluenzaModelParams.STUDENTS){
			// TODO programar actividades de otros agentes.
			this.type = AgentType.KID;
		}
		for (Building b : ContextManager.buildingContext.getRandomObjects(Building.class, 10000)) {
            // See if the building is a bank (they will have type==2).
            if (b.getType()==4) {
                    this.funplaces.add( b );
                    //break; // Have found a bank, stop searching.
            }
        }
		DefaultAgent.susceptibleCounter++;
		
		
	}

	@Override
	public void step() throws Exception {
		double theTime = BigDecimal.valueOf(ContextManager.realTime).
				round(new MathContext(5,RoundingMode.HALF_UP)).doubleValue();
		
		if(this.amIAbleToGoOut()){
			
			if (DefaultAgent.isContingencyOn){
				this.checkH1N1AgentStatus();
				if (!this.isInfected()){
					if(theTime == 10.0){
						doFunStuff();
					}
					else if (theTime == 17.0 ) { // 5pm, agent should go home
						goHome();
					} if (this.route == null) {
				        // Don't do anything if a route hasn't been created.
					}else if (this.route.atDestination()) {
				        // Have reached our destination, lets delete the old route (more efficient).
					this.checkH1N1AgentStatus();
					GeographyWithin<DefaultAgent> aroundMe = new GeographyWithin<DefaultAgent>(ContextManager.getAgentGeography(),5, this);
					//if ((this.status == AgentStatus.EXPOSED) || (this.status == AgentStatus.INFECTED)){
					if ((this.status == AgentStatus.INFECTED)){
						this.infect_and_check_pop(aroundMe, this.route.getDestinationBuilding().getDensity());
					}
					this.currentBuilding = this.route.getDestinationBuilding();
					//this.workingTimeStart = ContextManager.realTime;
				    this.route = null;
				}
				else {
					try {
						this.route.travel();
					} catch (Exception ex){
						//ContextManager.LogFile.logException("LINEA 181 " + this.name +this.type +  " EX: " +ex.toString() + this.currentBuilding.getIdentifier()+" ->"+this.route.getDestinationBuilding().getIdentifier());
					}
				}
				} 
				
			} 
			else {
/////////////WORKER
				if (type == AgentType.WORKER) {
					if (theTime == 8.0) { // 8am, Agent should be working
						goToWork(this.workplace);
					}
					if (theTime == 14.0){
						doFunStuff();
					}
					//else if (theTime == 17.0 && ContextManager.realTime > (this.workingTimeStart + 8.0)) { // 5pm, agent should go home
					else if (theTime == 17.0 ) { // 5pm, agent should go home
						goHome();
					}
					if (this.route == null) {
					        // Don't do anything if a route hasn't been created.
					} else if (this.route.atDestination()) {
					        // Have reached our destination, lets delete the old route (more efficient).
						this.checkH1N1AgentStatus();
						GeographyWithin<DefaultAgent> aroundMe = new GeographyWithin<DefaultAgent>(ContextManager.getAgentGeography(),5, this);
						//if ((this.status == AgentStatus.EXPOSED) || (this.status == AgentStatus.INFECTED)){
						if ((this.status == AgentStatus.INFECTED)){
							this.infect_and_check_pop(aroundMe, this.route.getDestinationBuilding().getDensity());
						}
						this.currentBuilding = this.route.getDestinationBuilding();
						this.workingTimeStart = ContextManager.realTime;
					    this.route = null;
					}
					else {
						try {
							this.route.travel();
						} catch (Exception ex){
							//ContextManager.LogFile.logException("LINEA 181 " + this.name +this.type +  " EX: " +ex.toString() + this.currentBuilding.getIdentifier()+" ->"+this.route.getDestinationBuilding().getIdentifier());
						}
					}
				} 
//////////STUDENT
				else if (type == AgentType.STUDENT){
					
					if (theTime == 9.0) { // 8am, Agent should be working
						goToSchool(this.workplace);
					}
					if (theTime == 14.0){
						doFunStuff();
					}
					else if (theTime == 19.0) { // 5pm, agent should go home
						goHome(); // Create a route home
					}
					if (this.route == null) {
					        // Don't do anything if a route hasn't been created.
					} else if (this.route.atDestination()) {
						GeographyWithin<DefaultAgent> aroundMe = new GeographyWithin<DefaultAgent>(ContextManager.getAgentGeography(),2, this);
				
						//if ((this.status == AgentStatus.EXPOSED) || (this.status == AgentStatus.INFECTED)){
						if ((this.status == AgentStatus.INFECTED)){
							this.infect_and_check_pop(aroundMe, this.route.getDestinationBuilding().getDensity());
						}
						this.currentBuilding = this.route.getDestinationBuilding();
					    this.route = null;
					}
					else {
						try {
							this.route.travel();
						} catch (Exception ex){
							//ContextManager.LogFile.logException("LINEA 211" + this.name+this.type + " Exception: " + ex.getMessage() +this.currentBuilding.getIdentifier()+" ->"+this.route.getDestinationBuilding().getIdentifier());
						}
					}
				}
/////////////////OTHER
				else if (type == AgentType.KID) {
					if (theTime == 12.0) {
						doFunStuff();
					} 
					if (theTime == 15.0) {
						goHome();
					}
					if (this.route == null){
						
					} else if (this.route.atDestination()) {
				        // Have reached our destination, lets delete the old route (more efficient).
						this.checkH1N1AgentStatus();
						GeographyWithin<DefaultAgent> aroundMe = new GeographyWithin<DefaultAgent>(ContextManager.getAgentGeography(),5, this);
						//if ((this.status == AgentStatus.EXPOSED) || (this.status == AgentStatus.INFECTED)){
						if ((this.status == AgentStatus.INFECTED)){
							this.infect_and_check_pop(aroundMe, this.route.getDestinationBuilding().getDensity());
						}
						this.currentBuilding = this.route.getDestinationBuilding();
						this.workingTimeStart = ContextManager.realTime;
					    this.route = null;
					}
					else {
						try {
							this.route.travel();
						} catch (Exception ex){
							//ContextManager.LogFile.logException("LINEA 181 " + this.name +this.type +  " EX: " +ex.toString() + this.currentBuilding.getIdentifier()+" ->"+this.route.getDestinationBuilding().getIdentifier());
						}
					}
				}
			}

		}
		//No puedo salir porque estoy enfermo, me quedare en casa sin hacer nada
		else{
			
		}
		this.checkH1N1AgentStatus();
		
		
	} // step()
	
	private void doFunStuff(){
		boolean val = new Random().nextInt(4)==0;
		if (val){
			if (funplaces.size() > 0){
				Random rnd = new Random();
				Building b = funplaces.get(rnd.nextInt(funplaces.size()));
				this.goToFun(b);
			}
		}
//		} else {
//			this.route = null;
//		}
	}
	
	/***
	 * Permite averiguar el estado del agente cuando se encuentra enfermo utilizando el valor INFLUENCE_VALUE de la infeccion.
	 * A mayor INFLUENCE_VALUE, el agente sale menos de su hogar.
	 * @return si el agente tiene ganas de salir o no.
	 */
	private boolean amIAbleToGoOut(){
		if(this.isInfected()){
			return Math.random()<= GlobalVars.InfluenzaModelParams.INFLUENCE_VALUE ? false :  true;
		} else
			return true;
	}


	/**
	 * There will be no inter-agent communication so these agents can be executed simulataneously in separate threads.
	 */
	@Override
	public final boolean isThreadable() {
		return true;
	}

	@Override
	public void setHome(Building home) {
		this.home = home;
	}

	@Override
	public Building getHome() {
		return this.home;
	}

	@Override
	public <T> void addToMemory(List<T> objects, Class<T> clazz) {
	}

	@Override
	public List<String> getTransportAvailable() {
		return null;
	}

	@Override
	public String toString() {
		return "Agent " + this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultAgent))
			return false;
		DefaultAgent b = (DefaultAgent) obj;
		return this.id == b.id;
	}

	@Override
	public int hashCode() {
		return this.id;
	}
	
	public AgentType getType(){
		return this.type;
	}
	
	public AgentStatus getStatus(){
		return this.status;
	}
	////////////////////////////SCHEDULE
	
	public void goToWork(Building workspace){
		this.route = new Route(this, workspace.getCoords(), workspace);
	}
	
	public void goToSchool(Building school){
		this.route =  new Route(this,school.getCoords(), school);
	}
	
	public void goToFun(Building funplace){
		this.route=  new Route(this, funplace.getCoords(), funplace);
	}
	
	public void goHome(){
		try {
			this.route = new Route(this, this.home.getCoords(), this.home);
		} catch (Exception ex){
			ContextManager.LogFile.logException("LINEA 319" + this.name +this.type + " Building: " + this.route );
		}
	}

	
	////////////////////////////
	
	public int getDaysInfected(){
		if (this.isSuceptible())
			return 0;
		else
			return this.daysInfected;
	}
	
	public boolean isSuceptible(){
		if (this.status == AgentStatus.SUSCEPTIBLE )
			return true;
		else
			return false;
	}
	public boolean isExposed() {
		if (this.status == AgentStatus.EXPOSED )
			return true;
		else 
			return false;
					
	}
	public boolean isInfected(){
		if (this.status == AgentStatus.INFECTED ) {
			return true;
		} else 
			return false;
	}
	
	public boolean isCured(){
		if ( this.status == AgentStatus.RECOVERED ){
			return true;
		} else
			return false;
	}
	
	/**
	 * Metodo que le comunica al agente que ha sido infectado, intercambiando su
	 * estado de Susceptible a expuesto asi como asignando el periodo de incubacion 
	 * del virus
	 * @param infected referencia al agente que me infecto
	 */
	public void expose(DefaultAgent infected){
		if (infected.status == AgentStatus.SUSCEPTIBLE){
			infected.status = AgentStatus.EXPOSED;
			//infected.whoInfectedMe = infected;
			Random rand = new Random();
			infected.incubationPeriod = GlobalVars.InfluenzaModelParams.EXPOSED_DAYS[rand.nextInt(GlobalVars.InfluenzaModelParams.EXPOSED_DAYS.length)];
			//if (DefaultAgent.susceptibleCounter != 0 )
			//	DefaultAgent.susceptibleCounter--;
			DefaultAgent.exposedCounter++;
		}
	}
	
	public void infect(DefaultAgent infected){
		if ( infected.status == AgentStatus.EXPOSED ) {
			infected.status = AgentStatus.INFECTED;
			Random rand = new Random();
			infected.symptomsPeriod = GlobalVars.InfluenzaModelParams.INFECTED_DAYS[rand.nextInt(GlobalVars.InfluenzaModelParams.INFECTED_DAYS.length)];
			//DefaultAgent.exposedCounter--;
			DefaultAgent.infectedCounter++;
		}
	}
	
	public void cure(DefaultAgent infected) {
		if ( infected.status == AgentStatus.INFECTED) {
			infected.status = AgentStatus.RECOVERED;
			//DefaultAgent.infectedCounter--;
			DefaultAgent.recoveredCounter++;
		}
	}
	

	/**
	 * Metodo que verifica el desarrollo de la enfermedad -- SEIR
	 */
	private void checkH1N1AgentStatus() {
		// Agentes SUCEPTIBLES
		if (this.isSuceptible())
			return;
		else {
			//Agentes Expuestos
			if (this.isExposed())
				checkH1N1ExposedAgentStatus();
			//Agentes Infectados
			else if (this.isInfected())
				checkH1N1InfectedAgentStatus();
			//Agentes Recuperados
			else if (this.isCured())
				checkH1N1RecoveredAgentStatus();
		}
		
	}

	private void checkH1N1RecoveredAgentStatus() {
		// TODO Auto-generated method stub
		
	}

	private void checkH1N1InfectedAgentStatus() {
//		if ( ContextManager.getRealTime() == 23.0){
			if ( this.daysInfected >= (this.incubationPeriod + this.symptomsPeriod)){
				this.cure(this);
				DefaultAgent.recoveredCounter++;
			} else {
				if (ContextManager.getRealTime() == 23.0)
				this.daysInfected ++;
			}
//		}
		
	}

	private void checkH1N1ExposedAgentStatus() {
		// TODO Auto-generated method stub
		double time = ContextManager.getRealTime();
//		if (ContextManager.getRealTime() == 23.0){
			if  ( this.daysExposed >= this.incubationPeriod ){
				Random rand = new Random();
				this.infect(this);
				
			} else {
				if (time == 23.0){
					this.daysExposed ++;
				}
			}
//		}
	}
	
	/**
	 * Metodo llamado por los agentes infectados (AgentStatus.INFECTED), revisa la densidad de poblacion
	 * dependiendo el tipo de edificios donde se encuentre el agente con el fin de infectar a una mayor 
	 * o menor cantidad de personas
	 * @param aroundMe la lista de agentes cerca de este agente
	 * @param typeBuilding el tipo de edificio en el que este agente se encuentra
	 */
	public void infect_and_check_pop(GeographyWithin<DefaultAgent> aroundMe, int typeBuilding){
		ArrayList<DefaultAgent> listaAgentes = null;
		listaAgentes = (ArrayList<DefaultAgent>)DefaultAgent.makeCollection(aroundMe.query());
		if (listaAgentes.size()>0){
			int rand = new Random().nextInt(listaAgentes.size());
			//Trabajo> 75% de probabilidad de infeccion
			if ( typeBuilding == 2){
				infection(GlobalVars.InfluenzaModelParams.HIGH_DENSITY, listaAgentes);
			}
			//Estudiantes> 50% de probabilidad de infeccion
			else if ( typeBuilding == 3){
				infection(GlobalVars.InfluenzaModelParams.MEDIUM_DENSITY, listaAgentes);
			}
			//Centros de recreacion
			else if (typeBuilding == 4) {
				infection(GlobalVars.InfluenzaModelParams.LOW_DENSITY, listaAgentes);
			}
		}
		
	}
	
	/**
	 * Metodo que permite al agente infectar a una lista de agentes cerca de el 
	 * dependiendo la densidad de poblacion. Este metodo permite comunicarse con los 
	 * agentes a infectar para cambiar su estado de susceptible a expuesto.
	 * @param popDensity
	 * @param aroundMe
	 */
	public void infection(double popDensity, ArrayList<DefaultAgent> aroundMe){
		//int infected = (int)(aroundMe.size()*popDensity);
		Random r = new Random();
		int rand = r.nextInt(100);
		if ( rand < popDensity*100){
			Collections.shuffle(aroundMe);
			Building b = this.route.getDestinationBuilding();
			//DefaultAgent a = aroundMe.get(new Random().nextInt(aroundMe.size()));
			for(int i = 0; i <= GlobalVars.InfluenzaModelParams.BASIC_REPRODUCTIVE_RATIO; i++ ){
				DefaultAgent a = aroundMe.get(i);
				//this.infectedList.add(a);
				//Inicio de proceso de enfermedad, agente 'a' expuesto
				this.expose(a);
			}
			b.infectionHere();
		}
//		int infected = (int)(aroundMe.size()*popDensity);
//		//infected = (aroundMe.size()-infected)*(int)GlobalVars.InfluenzaModelParams.BASIC_REPRODUCTIVE_RATIO;
//		Collections.shuffle(aroundMe);
//		Building b = this.route.getDestinationBuilding();
//		for(int i = 0; i < infected; i++ ){
//		//for(int i = 0; i <= (GlobalVars.InfluenzaModelParams.BASIC_REPRODUCTIVE_RATIO); i++ ){
//			DefaultAgent a = aroundMe.get(i);
//			this.infectedList.add(a);
//			//Inicio de proceso de enfermedad, agente 'a' expuesto
//			this.expose(a);
//		}
//		b.infectionHere();
	}

	public void randomInfection(){
		this.name = "ZERO";
		this.expose(this);
	}
	
	public static void randomInfection(DefaultAgent agente){
		agente.name = "ZERO";
		agente.expose(agente);
	}

	
	//Utilities
	
	public static <E> Collection<E> makeCollection(Iterable<E> iter){
	    Collection<E> list = new ArrayList<E>();
	    for (E item : iter) {
	        list.add(item);
	    }
	    return list;
	}

	public static int getSusceptibleCounter() {
		return susceptibleCounter;
	}


	public static int getExposedCounter() {
		return exposedCounter;
	}


	public static int getRecoveredCounter() {
		return recoveredCounter;
	}
	
	public static int getInfectedCounter(){
		return infectedCounter;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return this.name;
	}
	
//	public boolean isAtHome(){
//		if(this.)
//	}
	
	

}
