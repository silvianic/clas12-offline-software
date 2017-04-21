package org.jlab.rec.cvt.track.fit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.rec.cvt.svt.Constants;
import org.jlab.rec.cvt.svt.Geometry;
import org.jlab.rec.cvt.track.Seed;
import org.jlab.rec.cvt.trajectory.TrkSwimmer;

import Jama.Matrix;

public class StateVecs {

	final static double speedLight = 0.002997924580;
	
	public List<B> bfieldPoints = new ArrayList<B>();
	public Map<Integer, StateVec> trackTraj = new HashMap<Integer, StateVec>();
	public Map<Integer,CovMat> trackCov = new HashMap<Integer, CovMat>();

	public double stepSize = 0.2; // step size 
	public StateVec StateVec;
	public CovMat CovMat;
	public Matrix F;
	
	public List<Double>X0;
	public List<Double>Y0;
	public List<Double>Z0; // reference points
	
	public List<Integer>Layer;
	public List<Integer>Sector;
	
	public StateVec getStateVecAtModule(int k, StateVec kVec, Geometry geo) {
		
		StateVec newVec = kVec;
		double xc = X0.get(k) + (kVec.d_rho+kVec.alpha/kVec.kappa)*Math.cos(kVec.phi0);
		double yc = Y0.get(k) + (kVec.d_rho+kVec.alpha/kVec.kappa)*Math.sin(kVec.phi0);
		double r = Math.abs(kVec.alpha/kVec.kappa);
		
		// Find the intersection of the helix circle with the module plane projection in XY which is a line
		// Plane representative line equation y = mx +d
		Point3D Or =geo.getPlaneModuleOrigin(Sector.get(k), Layer.get(k));
		Point3D En =geo.getPlaneModuleEnd(Sector.get(k), Layer.get(k));
		double X = 0;
		double Y = 0;
		
		if(En.x()-Or.x()==0) {
			X = Or.x();
			double y1 = yc + Math.sqrt(r*r - (X - xc)*(X - xc));
			double y2 = yc - Math.sqrt(r*r - (X - xc)*(X - xc));
			
			if(Math.abs(y1-Or.y())<Math.abs(En.y()-Or.y())) {
				Y = y1;
			} else {
				if(Math.abs(y2-Or.y())<Math.abs(En.y()-Or.y())) {
					Y = y2;
				}
			}
		}
		if(En.y()-Or.y()==0) {
			Y = Or.y();
			double x1 = xc + Math.sqrt(r*r - (Y - yc)*(Y - yc));
			double x2 = xc - Math.sqrt(r*r - (Y - yc)*(Y - yc));
			
			if(Math.abs(x1-Or.x())<Math.abs(En.x()-Or.x())) {
				X = x1;
			} else {
				if(Math.abs(x2-Or.x())<Math.abs(En.x()-Or.x())) {
					X = x2;
				}
			}
		}
		
		if(En.x()-Or.x()!=0 && En.y()-Or.y()!=0) {
			double m = (En.y()-Or.y())/(En.x()-Or.x());
			double d = Or.y() - Or.x()*m;
			
			double del = r*r*(1+m*m) - (yc-m*xc-d)*(yc-m*xc-d);
			if(del<0)
				return kVec;
			double x1 = (xc+yc*m - d*m + Math.sqrt(del))/(1+m*m);
			double x2 = (xc+yc*m - d*m - Math.sqrt(del))/(1+m*m);
			
			if(Math.abs(x1-Or.x())<Math.abs(En.x()-Or.x())) {
				X = x1;
			} else {
				if(Math.abs(x2-Or.x())<Math.abs(En.x()-Or.x())) {
					X = x2;
				}
			}
			double y1 = yc + Math.sqrt(r*r - (X - xc)*(X - xc));
			double y2 = yc - Math.sqrt(r*r - (X - xc)*(X - xc));
			
			if(Math.abs(y1-Or.y())<Math.abs(En.y()-Or.y())) {
				Y = y1;
			} else {
				if(Math.abs(y2-Or.y())<Math.abs(En.y()-Or.y())) {
					Y = y2;
				}
			}
		}
		Vector3D ToRef = new Vector3D(X0.get(k) - xc, Y0.get(k) - yc, 0 ).asUnit();
		Vector3D ToPoint = new Vector3D(X - xc, Y - yc, 0 ).asUnit();
		
		double phi = ToRef.angle(ToPoint);
		
		double x = X0.get(k) + kVec.d_rho*Math.cos(kVec.phi0) + kVec.alpha *(Math.cos(kVec.phi0) - Math.cos(kVec.phi0 + phi));
		double y = Y0.get(k) + kVec.d_rho*Math.sin(kVec.phi0) + kVec.alpha *(Math.sin(kVec.phi0) - Math.sin(kVec.phi0 + phi));
		double z = Z0.get(k) + kVec.dz - kVec.alpha*kVec.tanL*phi;
		newVec.x = x;
		newVec.y = y;
		newVec.z = z;
		newVec.phi = phi;
		System.out.println(" x "+x+" y "+y+" z "+z+" O "+Or.toString());
		return newVec;
	}
	

	public void transport(int i, int f, StateVec iVec, CovMat icovMat, Geometry geo) { // s = signed step-size
		System.out.println("in transport ");
		B Bf = new B(i, iVec.x, iVec.y, iVec.z);
		System.out.println(" B "+Bf.Bz+"Z0.get(i)"+ Z0.get(i) +" Z0.get(f) "+Z0.get(f));
		double Xc = X0.get(i) + (iVec.d_rho+iVec.alpha/iVec.kappa)*Math.cos(iVec.phi0);
		double Yc = Y0.get(i) + (iVec.d_rho+iVec.alpha/iVec.kappa)*Math.sin(iVec.phi0);
		System.out.println("Xc "+Xc+" Yc "+Yc);
		// transport stateVec...
		StateVec fVec = new StateVec(f);
		
		double phi_f = Math.atan2(Yc - Y0.get(f), Xc - X0.get(f));
		if(iVec.kappa<0)
			phi_f = Math.atan2(-Yc + Y0.get(f), -Xc + X0.get(f));
		
		fVec.phi0 = phi_f;
		
		fVec.d_rho = (Xc - X0.get(f))*Math.cos(phi_f) + (Yc - Y0.get(f))*Math.sin(phi_f) - Bf.alpha/iVec.kappa ;
		
		fVec.kappa = ELoss_kappa(iVec, f-i) ;
		
		fVec.dz = Z0.get(i) - Z0.get(f) +iVec.dz -(Bf.alpha/iVec.kappa)*(phi_f - iVec.phi0)*iVec.tanL;
		
		fVec.tanL = iVec.tanL;
		
		fVec.alpha = Bf.alpha;
		
		fVec = this.getStateVecAtModule(f, fVec, geo);
	
		// now transport covMat...
		double dphi0_prm_del_drho = -1./(fVec.d_rho + iVec.alpha/iVec.kappa)*Math.sin(fVec.phi0 - iVec.phi0);
		double dphi0_prm_del_phi0 = (iVec.d_rho + iVec.alpha/iVec.kappa)/(fVec.d_rho + iVec.alpha/iVec.kappa)*Math.cos(fVec.phi0 - iVec.phi0);
		double dphi0_prm_del_kappa = (iVec.alpha/(iVec.kappa*iVec.kappa))/(fVec.d_rho + iVec.alpha/iVec.kappa)*Math.sin(fVec.phi0 - iVec.phi0);
		double dphi0_prm_del_dz = 0;
		double dphi0_prm_del_tanL = 0;
		
		double drho_prm_del_drho = Math.cos(fVec.phi0 - iVec.phi0);
		double drho_prm_del_phi0 = (iVec.d_rho + iVec.alpha/iVec.kappa)*Math.sin(fVec.phi0 - iVec.phi0);
		double drho_prm_del_kappa = (iVec.alpha/(iVec.kappa*iVec.kappa))*(1 -Math.cos(fVec.phi0 - iVec.phi0) );
		double drho_prm_del_dz = 0;
		double drho_prm_del_tanL = 0;
		
		double dkappa_prm_del_drho = 0;
		double dkappa_prm_del_phi0 = 0;
		double dkappa_prm_del_dkappa = 1;
		double dkappa_prm_del_dz = 0;
		double dkappa_prm_del_tanL = 0;
		
		double dz_prm_del_drho = ((iVec.alpha/iVec.kappa)/(fVec.dz + iVec.alpha/iVec.kappa))*iVec.tanL*Math.sin(fVec.phi0 - iVec.phi0);
		double dz_prm_del_phi0 = (iVec.alpha/iVec.kappa)*iVec.tanL*(1 - Math.cos(fVec.phi0 - iVec.phi0)*(iVec.dz + iVec.alpha/iVec.kappa)/(fVec.dz + iVec.alpha/iVec.kappa));
		double dz_prm_del_kappa = (iVec.alpha/(iVec.kappa*iVec.kappa))*iVec.tanL*(fVec.phi0 - iVec.phi0 - Math.sin(fVec.phi0 - iVec.phi0)*(iVec.alpha/iVec.kappa)/(fVec.dz + iVec.alpha/iVec.kappa));
		double dz_prm_del_dz = 1;
		double dz_prm_del_tanL = -iVec.alpha*(fVec.phi0 - iVec.phi0)/iVec.kappa;
		
		double dtanL_prm_del_drho = 0;
		double dtanL_prm_del_phi0 = 0;
		double dtanL_prm_del_dkappa = 0;
		double dtanL_prm_del_dz = 0;
		double dtanL_prm_del_tanL = 1;
		
		double[][] FMat =  new double[][] {
				{drho_prm_del_drho,   drho_prm_del_phi0,   drho_prm_del_kappa,    drho_prm_del_dz,   drho_prm_del_tanL},
				{dphi0_prm_del_drho,  dphi0_prm_del_phi0,  dphi0_prm_del_kappa,   dphi0_prm_del_dz,  dphi0_prm_del_tanL},				
				{dkappa_prm_del_drho, dkappa_prm_del_phi0, dkappa_prm_del_dkappa, dkappa_prm_del_dz, dkappa_prm_del_tanL},
				{dz_prm_del_drho,     dz_prm_del_phi0,     dz_prm_del_kappa,      dz_prm_del_dz,     dz_prm_del_tanL},
				{dtanL_prm_del_drho,  dtanL_prm_del_phi0,  dtanL_prm_del_dkappa,  dtanL_prm_del_dz,  dtanL_prm_del_tanL}
		};
		
		
		//StateVec = fVec;
		this.trackTraj.put(f, fVec); 
		F = new Matrix(FMat); 
		Matrix FT = F.transpose();
		Matrix Cpropagated = FT.times(icovMat.covMat).times(F);
		
		if(Cpropagated!=null) {
			CovMat fCov = new CovMat(f);
			fCov.covMat = Cpropagated.plus(this.Q(iVec, f-i));
			//CovMat = fCov;
			this.trackCov.put(f, fCov); 
		} 
	}	
	
	private double ELoss_kappa(StateVec iVec, int dir) {
		
		Vector3D trkDir = 	this.P(iVec.k);
		trkDir.unit();
		double cosEntranceAngle = this.P(iVec.k).z();
		
		double pt = Math.abs(1./iVec.kappa);
		double pz = pt*iVec.tanL;
		double p = Math.sqrt(pt*pt+pz*pz);
		
	    double mass = MassHypothesis(massHypo); // assume given mass hypothesis
	    double beta = p/Math.sqrt(p*p+mass*mass); // use particle momentum
	    double gamma = 1./Math.sqrt(1-beta*beta);
		
	    double s = MassHypothesis("electron")/mass;

 		double Wmax = 2.*mass*beta*beta*gamma*gamma/(1.+2.*s*gamma+s*s);
 		double I = 0.000000172;

 		double logterm = 2.*mass*beta*beta*gamma*gamma*Wmax/(I*I);

 		double delta = 0.; 
 		double dEdx = 0.00001535*Constants.detMatZ_ov_A_timesThickn*(Math.log(logterm)-2*beta*beta-delta)/(beta*beta); //in GeV/mm
 	
 		double tmpPtot = p;
		
	    double tmpEtot = Math.sqrt(MassHypothesis(massHypo)*MassHypothesis(massHypo)+tmpPtot*tmpPtot); 
	    double tmpEtotCorrected = tmpEtot-dir*dEdx/cosEntranceAngle; 
 	    double tmpPtotCorrSq = tmpEtotCorrected*tmpEtotCorrected-MassHypothesis(massHypo)*MassHypothesis(massHypo); 
	   
 	    double newPt = Math.sqrt(tmpPtotCorrSq/(1+iVec.tanL*iVec.tanL));
 	    
 	    return Math.signum(iVec.kappa)/newPt ;		
	}
	
	private Matrix Q(StateVec iVec, int dir) {

		 Matrix Q = new Matrix( new double[][]{
					{0, 0, 						0,   	  							0,      0									},
					{0, 0,  					0,          						0,      0									},
					{0, 0, 		  				0,									0,		0									},
					{0, 0, 						0, 									0, 		0									},
					{0, 0, 						0, 									0,		0									}
			});
		 
		
		if(iVec.k%2==1 && dir>0 ) {
			Vector3D trkDir = 	this.P(iVec.k); 
			trkDir.unit();
			double cosEntranceAngle = Math.abs(this.P(iVec.k).z());
			
			double pt = Math.abs(1./iVec.kappa);
			double pz = pt*iVec.tanL;
			double p = Math.sqrt(pt*pt+pz*pz);
			
		    double t_ov_X0 = 2.*0.32/Constants.SILICONRADLEN; //path length in radiation length units = t/X0 [true path length/ X0] ; Si radiation length = 9.36 cm
		    
		    double mass = MassHypothesis(massHypo); // assume given mass hypothesis
		    double beta = p/Math.sqrt(p*p+mass*mass); // use particle momentum
		    double pathLength = t_ov_X0/cosEntranceAngle;  
		    
		    double sctRMS =(0.0136/(beta*p))*Math.sqrt(pathLength)*(1+0.038*Math.log(pathLength)); // Highland-Lynch-Dahl formula
		   
		    Q = new Matrix( new double[][]{
					{0, 0, 						0,   	  							0,      0									},
					{0, sctRMS*(1+iVec.tanL*iVec.tanL),  	0,          						0,      0									},
					{0, 0, 		  				sctRMS*(iVec.kappa*iVec.kappa*iVec.tanL*iVec.tanL),		0,		sctRMS*(iVec.kappa*iVec.tanL*(1+iVec.tanL*iVec.tanL))	},
					{0, 0, 						0, 									0, 		0									},
					{0, 0, 						sctRMS*(iVec.kappa*iVec.tanL*(1+iVec.tanL*iVec.tanL)),  0,		sctRMS*(1+iVec.tanL*iVec.tanL)*(1+iVec.tanL*iVec.tanL)	}
			});
		}
	    return Q;
	    
	    
	}
	
	
	
	public class StateVec {
		final int k;
		
		public double x;
		public double y;
		public double z;
		public double kappa;
		public double d_rho;
		public double phi0;
		public double phi;
		public double tanL;
		public double dz;
		public double alpha;
		
		StateVec(int k){
			this.k = k;
		}
		
	}
	
	public class CovMat {
		final int k;
		public Matrix covMat;
		
		CovMat(int k){
			this.k = k;
		}
		
	}
	
    TrkSwimmer tSwim = new TrkSwimmer();
	
	public class B {
		final int k;		
		double x;
		double y;
		double z;
		
		public double Bx;
		public double By;
		public double Bz;
		
		public double alpha;
		
		B(int k, double x, double y, double z) {
			this.k = k;
			this.x = x;
			this.y = y;
			this.z = z; 
			
			Point3D bf = tSwim.Bfield(x/10,y/10,z/10);
			this.Bx = bf.x();
			this.By = bf.y();
			this.Bz = bf.z();
			
			this.alpha = 1./(StateVecs.speedLight * bf.z());
		}		
	}
	
	public String massHypo = "electron";
	
	public double MassHypothesis(String H) {
    	double piMass = 0.13957018;
   	  	double KMass  = 0.493677;
   	  	double muMass = 0.105658369;
   	  	double eMass  = 0.000510998;
   	  	double pMass  = 0.938272029;
   	  	double value = piMass; //default
   	  	if(H.equals("proton"))
  		  value = pMass;
   	  	if(H.equals("electron"))
  		  value = eMass;
   	  	if(H.equals("pion"))
  		  value = piMass;
   	  	if(H.equals("kaon"))
  		  value = KMass;
   	  	if(H.equals("muon"))
  		  value = muMass;
   	  	return value;
     }
	
	
	
	public Vector3D P(int kf) {
		if(this.trackTraj.get(kf)!=null) {
			//double x = this.trackTraj.get(kf).x;
			//double y = this.trackTraj.get(kf).y;
			//double z = this.trackTraj.get(kf).z; 
			//B Bf = new B(kf, x, y, z);
			double px = -Math.signum(1/this.trackTraj.get(kf).kappa)*Math.sin(this.trackTraj.get(kf).phi0+this.trackTraj.get(kf).phi);
			double py = Math.signum(1/this.trackTraj.get(kf).kappa)*Math.cos(this.trackTraj.get(kf).phi0+this.trackTraj.get(kf).phi);
			double pz = Math.signum(1/this.trackTraj.get(kf).kappa)*this.trackTraj.get(kf).tanL;
			//int q = (int) Math.signum(this.trackTraj.get(kf).kappa);
			
			return new Vector3D(px,py,pz);
		} else {
			return new Vector3D(0,0,0);
		}
		
	}
	
	public void init(Seed trk, KFitter kf) {
		//init stateVec
		StateVec initSV = new StateVec(0);
		initSV.x = -trk.get_Helix().get_dca()*Math.sin(trk.get_Helix().get_phi_at_dca());
		initSV.y = trk.get_Helix().get_dca()*Math.cos(trk.get_Helix().get_phi_at_dca());
		initSV.z = trk.get_Helix().get_Z0();
		double xcen    = (1./trk.get_Helix().get_curvature() - trk.get_Helix().get_dca()) * Math.sin(trk.get_Helix().get_phi_at_dca());  
        double ycen    = (-1./trk.get_Helix().get_curvature() + trk.get_Helix().get_dca()) * Math.cos(trk.get_Helix().get_phi_at_dca()); 
        B Bf = new B(0,0,0,0);
        initSV.alpha = Bf.alpha;
        initSV.kappa = Bf.alpha*trk.get_Helix().get_curvature();      
		initSV.phi0 = Math.atan2(ycen, xcen);
		if(initSV.kappa<0)
			initSV.phi0 = Math.atan2(-ycen, -xcen);
		initSV.dz = trk.get_Helix().get_Z0();
		initSV.tanL = trk.get_Helix().get_tandip();
		initSV.d_rho = trk.get_Helix().get_dca();
		initSV.phi = 0;
		this.trackTraj.put(0, initSV);	
		//init covMat
		Matrix fitCovMat = 	trk.get_Helix().get_covmatrix();
		double cov_d02 = fitCovMat.get(0, 0);
		double cov_d0phi0 = fitCovMat.get(0, 1);
		double cov_d0rho = Bf.alpha*fitCovMat.get(0, 2);
		double cov_phi02 = fitCovMat.get(1, 1);
		double cov_phi0rho = Bf.alpha*fitCovMat.get(1, 2);
		double cov_rho2 = Bf.alpha*Bf.alpha*fitCovMat.get(2, 2);
		double cov_z02 = fitCovMat.get(3, 3);
		double cov_z0tandip = fitCovMat.get(3, 4);
		double cov_tandip2 = fitCovMat.get(4, 4);
		
		double components[][] = new double[5][5];
		for (int i = 0; i<5; i++)
		        for (int j = 0; j<5; j++)
		                components[i][j] = 0;
		
		components[0][0] = cov_d02;
		components[0][1] = cov_d0phi0;
		components[1][0] = cov_d0phi0;
		components[1][1] = cov_phi02;
		components[2][0] = cov_d0rho;
		components[0][2] = cov_d0rho;
		components[2][1] = cov_phi0rho;
		components[1][2] = cov_phi0rho;
		components[2][2] = cov_rho2;
		components[3][3] = cov_z02;
		components[3][4] = cov_z0tandip;
		components[4][3] = cov_z0tandip;
		components[4][4] = cov_tandip2;
		
		
		Matrix initCMatrix = new Matrix(components);
		
		CovMat initCM = new CovMat(0);
		initCM.covMat = initCMatrix;
		System.out.println(" init cov ");this.printMatrix(initCMatrix);
		this.trackCov.put(0, initCM); 
	}
	
	public void printMatrix(Matrix C) {
		for(int k = 0; k< 5; k++) {
			System.out.println(C.get(k, 0)+"	"+C.get(k, 1)+"	"+C.get(k, 2)+"	"+C.get(k, 3)+"	"+C.get(k, 4));
		}
	}
	public void printlnStateVec(StateVec S) {
		System.out.println(S.k+") drho "+S.d_rho+" phi0 "+S.phi0+" kappa "+S.kappa+" dz "+S.dz+" tanL "+S.tanL+" phi "+S.phi+" x "+S.x+" y "+S.y+" z "+S.z+" alpha "+S.alpha);
	}
}

