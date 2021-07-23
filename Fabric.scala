package NoC

import chisel3._
import scala.language.reflectiveCalls
import chisel3.util._
import Math._
import scala.collection.mutable.HashMap
import scala.collection.immutable.Vector
import chisel3.iotesters.PeekPokeTester

class Fabric extends Module with NoCInternalParams {
    val io = IO(new FabricIO)
    // Instantiate the router, AU and DisAU
    val router = new HashMap[Vector[Int], NoCRouter]() //Each Router will be identified with a Unique Vector(x,y)
    val toNoC = new HashMap[Vector[Int], ToNoCPayloadWithOverallParity]()
    val fromNoC = new HashMap[Vector[Int], FromNoCPayloadWithOverallParity]()

    //    val bistNI = new HashMap[Vector[Int], bistNI]()
		
		    // Construct The set of node
    val node = for(y <- 0 until ROWS; x <- 0 until COLS) yield { Vector(y,x) }
    var	x = 0
    var y = 0
/*		var timeout = 0

		var xWidth = log2Ceil(COLS)
		var yWidth = log2Ceil(ROWS)

		d(x: Int): Int = {
			var relX = 0
			var dx = COLS - 1 - x
			if(dx < (COLS/2)) {
				relX = dx
			} else if(dx == COLS/2) {
				if((COLS % 2) == 0) {
					relX = -dx
				} else {
					relX = dx
				}
			} else {	
			relX = -dx % (COLS/2)
			//	relX = -(COLS-dx)
			}
			relX
		}
	
		def calDy(y: Int): Int = {
			var relY = 0
			var dy = 0 - y
			if(dy < -(ROWS/2)) {
			relY = (-dy) % (ROWS/2)
			//relY = (dy+ROWS)
			} else {
				relY = dy
			}
			relY
		}
		
		var relX = calDx(x)
		var relY = calDy(y)*/

    // Iterate Over the number of Nodes to Make Connections
    node foreach { n => {
        x = n(1)
        y = n(0)
        // NoC Router instantiation
        if(x == 0 && y == 0) { 
            router += n -> Module( NoCRouterOrigin(x,y) ) //All RouterTypes are instantiated in NoCRouter.scala
        }
        else if (x == 0) { 
//            router += n -> Module( NoCRouterWest(x,y,timeout) )
            router += n -> Module( NoCRouterWest(x,y) )
        }
        else if(y == 0) {
            //router += n -> Module( NoCRouterNorth(x,y,timeout) )
            router += n -> Module( NoCRouterNorth(x,y) )
			//	if(x == COLS - 1) {
			//		io.eastIn.bits.v := router(n).io.inFlit(1).bits.v
		//		io.eastIn.bits.laneNo := router(n).io.inFlit(1).bits.laneNo
		//		io.eastIn.valid := router(n).io.inFlit(1).valid
		//		io.ejectOut.bits.v := router(n).io.outFlit(0).bits.v
		//		io.ejectOut.bits.laneNo := router(n).io.outFlit(0).bits.laneNo
		//		io.ejectOut.valid := router(n).io.outFlit(0).valid
				//	}
        }
        
	//	timeout = timeout + abs(relX) + abs(relY) + 3
	else if (x== 0 && y == (ROWS-1)) {
	router += n -> Module(NoCRouterSW(x,y) )
	}
	else if (y == ROWS-1){
	router += n -> Module(NoCRouterSouth(x,y) )
	}
	else if (x == COLS-1 && y == ROWS-1) {
	router += n -> Module(NoCRouterSE(x,y))
	}
	else if (x == COLS-1){
	router += n -> Module(NoCRouterEast(x,y))
	}
	else if (x== (COLS-1) && y == 0){
	router += n -> Module(NoCRouterNE(x,y))
	}
	else {
	router += n -> Module(NoCRouterNormal(x,y))
	}
    }}

    node foreach {
        n => {
            x = n(1)
            y = n(0)
            
            //AU/DAU Connection
            toNoC += n -> Module(new ToNoCPayloadWithOverallParity(x,y)) //toNoC = toNoC + n
            fromNoC += n -> Module(new FromNoCPayloadWithOverallParity(x,y))
           // bistNI += n -> Module(new bistNI(x,y))
            
            io.intoFabric(x)(y).inPacket <> toNoC(n).io.inPacket
            io.fromFabric(x)(y).outPacket <> fromNoC(n).io.outPacket
            
            // Establish Connection with the Network Interface and the Router
           router(n).io.inFlit(PortDir('EJECT)) <> toNoC(n).io.outFlit  // TODO Change made here 
//router(n).io.inFlit(PortDir('EJECT)).valid <> Mux(bistNI(n).io.active, bistNI(n).io.outFlit.valid, toNoC(n).io.outFlit.valid)
//router(n).io.inFlit(PortDir('EJECT)).bits <> Mux(bistNI(n).io.active, bistNI(n).io.outFlit.bits, toNoC(n).io.outFlit.bits)
	//router(n).io.inFlit(PortDir('EJECT)).vcStatus <> toNoC(n).io.outFlit.vcStatus
//			bistNI(n).io.inFlit.bits <> router(n).io.outFlit(PortDir('EJECT)).bits
//			bistNI(n).io.inFlit.valid <> router(n).io.outFlit(PortDir('EJECT)).valid

            		fromNoC(n).io.inFlit <> router(n).io.outFlit(PortDir('EJECT))
            
            // East-West Connection between routers
            if (x < COLS - 1) {// Inducing fault
          		if(x == 0){
                //println("NoCRouterOrigin, EastRouters,SouthWestRouters in
                //West Direction:  ")
        			router(n).io.inFlit(PortDir('EAST)) <> router(Vector(y, x + 1)).io.outFlit(PortDir('WEST)) 
        			router(n).io.outFlit(PortDir('EAST)) <> router(Vector(y, x + 1)).io.inFlit(PortDir('WEST))
              println("[info] [EAST-WEST] CONNECTING NODE("+x+","+y+") and NODE("+(x+1)+","+y+")")
	
/*                router(Vector(y, x + 1)).io.inFlit(PortDir('WEST)).bits.v(23,15) <> router(n).io.outFlit(PortDir('EAST)).bits.v(23,15)
		router(n).io.outFlit(PortDir('EAST)).bits.v(14)	:=  0.U
		router(Vector(y,x+1)).io.inFlit(PortDir('WEST)).bits.v(14) :=  0.U

                router(n).io.outFlit(PortDir('EAST)).bits.v(13,0) :=  router(Vector(y, x + 1)).io.inFlit(PortDir('WEST)).bits.v(13,0)
		router(n).io.outFlit(PortDir('EAST)).valid <> router(Vector(y, x + 1)).io.inFlit(PortDir('WEST)).valid
		
		router(n).io.outFlit(PortDir('EAST)).vcStatus <> router(Vector(y, x + 1)).io.inFlit(PortDir('WEST)).vcStatus
*/	}else{

	
                router(n).io.inFlit(PortDir('EAST)) <> router(Vector(y, x + 1)).io.outFlit(PortDir('WEST)) // Both forward and backward connections
                router(n).io.outFlit(PortDir('EAST)) <> router(Vector(y, x + 1)).io.inFlit(PortDir('WEST))
                println("[info] [EAST-WEST] CONNECTING NODE("+x+","+y+") and NODE("+(x+1)+","+y+")")
}
            }
            else {//x is equal to COLS-1
                // Toroidal connection
                router(Vector(y, 0)).io.inFlit(PortDir('WEST)) <> router(Vector(y, COLS - 1)).io.outFlit(PortDir('EAST))
                router(Vector(y, 0)).io.outFlit(PortDir('WEST)) <> router(Vector(y, COLS - 1)).io.inFlit(PortDir('EAST))
                println("[info] [EAST-WEST] CONNECTING NODE("+0+","+y+") and NODE("+(COLS - 1)+","+y+")")
            }
            
            if (y < ROWS - 1) {
                router(n).io.inFlit(PortDir('SOUTH)) <> router(Vector(y + 1, x)).io.outFlit(PortDir('NORTH)) // Both forward and backward connections
                router(n).io.outFlit(PortDir('SOUTH)) <> router(Vector(y + 1, x)).io.inFlit(PortDir('NORTH))
                println("[info] [SOUTH-NORTH] CONNECTING NODE("+x+","+y+") and NODE("+x+","+(y+1)+")")
            }
            else {
                // Toroidal connection
                router(Vector(0, x)).io.inFlit(PortDir('NORTH)) <> router(Vector((ROWS - 1), x)).io.outFlit(PortDir('SOUTH))
                router(Vector(0, x)).io.outFlit(PortDir('NORTH)) <> router(Vector((ROWS - 1), x)).io.inFlit(PortDir('SOUTH))
                println("[info] [SOUTH-NORTH] CONNECTING NODE("+x+","+0+") and NODE("+x+","+(ROWS - 1)+")")
            }
        }
    }
}

// Used for multi-FPGA device connections
/*class FabricTx[T <: Data](gen : T, colS : Int, colE : Int, rowS : Int, rowE : Int) extends Module with NoCInternalParams {
    val io = IO(new Bundle {
        val port = Vec(colE-colS+1, Vec(rowE-rowS+1, new UserFabricPortIO(gen)))

        val westInFlit = Flipped(Vec(rowE-rowS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES)))
        val westOutFlit = Vec(rowE-rowS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES))

        val eastInFlit = Flipped(Vec(rowE-rowS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES)))
        val eastOutFlit = Vec(rowE-rowS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES))

        val northInFlit = Flipped(Vec(colE-colS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES)))
        val northOutFlit = Vec(colE-colS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES))

        val southInFlit = Flipped(Vec(colE-colS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES)))
        val southOutFlit = Vec(colE-colS+1, DecoupledVCIO(new Flit, NUMVCS, NUMLANES))
    })
    
    val router = new HashMap[Vector[Int], NoCRouter]()
    val toNoC = new HashMap[Vector[Int], ToNoC]()
    val fromNoC = new HashMap[Vector[Int], FromNoC]()
    
    val node = for(y <- rowS to rowE; x <- colS to colE) yield { Vector(y,x) }
    var x = 0
    var y = 0
   // var timeout = 0
    
    // Initialise the vectors
    node foreach { 
        n => {
            x = n(1)
            y = n(0)
            if(x==0 && y==0) {
                router += n -> Module(NoCRouterOrigin(x,y)) 
            }
            else if (x == 0) {
                router += n -> Module(NoCRouterWest(x,y))
            }
            else if (y == 0) {
                router += n -> Module(NoCRouterNorth(x,y))
            }
            else {
                router += n -> Module(NoCRouterNormal(x,y))
            }
            toNoC += n -> Module(new ToNoC(x,y))
            fromNoC += n -> Module(new FromNoC(x,y))
        }
    }
    
    node foreach {
        n => {
            x = n(1)
            y = n(0)
            
            // Expose the network interface to the outside world
            for( j <- 0 until NUMLANES) {
                toNoC(n).io.inPacket(j).bits connectUserPacketToPacket io.port(x-colS)(y-rowS).inPacket(j).bits
                toNoC(n).io.inPacket(j).valid := io.port(x-colS)(y-rowS).inPacket(j).valid
                io.port(x-colS)(y-rowS).inPacket(j).ready := toNoC(n).io.inPacket(j).ready
                
                io.port(x-colS)(y-rowS).outPacket(j).bits connectPacketToUserPacket fromNoC(n).io.outPacket(j).bits
                io.port(x-colS)(y-rowS).outPacket(j).valid := fromNoC(n).io.outPacket(j).valid
                fromNoC(n).io.outPacket(j).ready := io.port(x-colS)(y-rowS).outPacket(j).ready
            }
            
            // Establish connection between network interface and router
            router(n).io.inFlit(PortDir('EJECT)) <> toNoC(n).io.outFlit
            fromNoC(n).io.inFlit <> router(n).io.outFlit(PortDir('EJECT))
            
            if (x <= colE - 1) {
                router(n).io.outFlit(PortDir('EAST)) <> router(Vector(y, x + 1)).io.inFlit(PortDir('WEST))
                router(n).io.inFlit(PortDir('EAST)) <> router(Vector(y, x + 1)).io.outFlit(PortDir('WEST))
            }
            else { // Toroidal connections exposed
                // East Connection exposed
                router(Vector(y,colE)).io.inFlit(PortDir('EAST)) <> io.eastInFlit(y - rowS)
                router(Vector(y,colE)).io.outFlit(PortDir('EAST)) <> io.eastOutFlit(y - rowS)
                
                // West connection exposed
                router(Vector(y,colS)).io.inFlit(PortDir('WEST)) <> io.westInFlit(y - rowS)
                router(Vector(y,colS)).io.outFlit(PortDir('WEST)) <> io.westOutFlit(y - rowS)
            }
            if (y <= rowE - 1) {
                router(n).io.inFlit(PortDir('SOUTH)) <> router(Vector(y + 1, x)).io.outFlit(PortDir('NORTH))
                router(n).io.outFlit(PortDir('SOUTH)) <> router(Vector(y + 1, x)).io.inFlit(PortDir('NORTH))
            }
            else {
                // North Connection exposed
                router(Vector(rowS,x)).io.inFlit(PortDir('NORTH)) <> io.northInFlit(x - colS)
                router(Vector(rowS,x)).io.outFlit(PortDir('NORTH)) <> io.northOutFlit(x - colS)
                
                // South connection exposed
                router(Vector(rowE,x)).io.inFlit(PortDir('SOUTH)) <> io.southInFlit(x - colS)
                router(Vector(rowE,x)).io.outFlit(PortDir('SOUTH)) <> io.southOutFlit(x - colS)
            }
        }
    }
}*/

class FabricTest(c: Fabric) extends PeekPokeTester(c) with NoCInternalParams {

def traffic_gen(injection_rate: Double, x: Int, y:Int, lane: Int) = {
    poke(c.io.intoFabric(x)(y).inPacket(lane).bits.laneNo, lane)
    poke(c.io.intoFabric(x)(y).inPacket(lane).bits.payloadSize, 1)
    val r = new scala.util.Random	
    var exec = injection_rate * 100 
    var cntr = 0

    for(i <- 0 until 100){
        if(cntr == 100/exec - 1)
        {
        poke(c.io.intoFabric(x)(y).inPacket(lane).valid, 1)
        cntr = 0
        var destX = r.nextInt(2) - 1//-1, 0
        var destY = r.nextInt(2) - 1//-1, 0
        if(destY == 0 && destX == 0)
        {
            destY = destY + 1
        }
        poke(c.io.intoFabric(x)(y).inPacket(lane).bits.destAddressX, destX)
        poke(c.io.intoFabric(x)(y).inPacket(lane).bits.destAddressY, destY)
        poke(c.io.intoFabric(x)(y).inPacket(lane).bits.payload, i)
        step(1)
        poke(c.io.intoFabric(x)(y).inPacket(lane).valid, 0)
        }
        else{
            cntr = cntr + 1
            step(1)
        }
        for(i<- 0 until COLS){
            for(j<- 0 until ROWS){
                peek(c.io.fromFabric(i)(j).outPacket(lane).bits)
                peek(c.io.fromFabric(i)(j).outPacket(lane).valid)
            }
        }

    }
    for(i<- 0 until 10)
    {
        step(1)
        for(i<- 0 until COLS){
            for(j<- 0 until ROWS){
                peek(c.io.fromFabric(i)(j).outPacket(lane).bits)
                peek(c.io.fromFabric(i)(j).outPacket(lane).valid)
            }
        }   
    }
}
for(i <- 0 until COLS) {
for(j <- 0 until ROWS) {
    poke(c.io.intoFabric(i)(j).inPacket(0).valid, 0)
    poke(c.io.intoFabric(i)(j).inPacket(1).valid, 0)
    poke(c.io.fromFabric(i)(j).outPacket(0).ready, 1)
    poke(c.io.fromFabric(i)(j).outPacket(1).ready, 1)
}
}
traffic_gen(0.1, 0, 0, 0)
/*
	
poke(c.io.intoFabric(1)(0).inPacket(0).bits.laneNo, 0)
poke(c.io.intoFabric(1)(0).inPacket(0).bits.destAddressX, -1)
poke(c.io.intoFabric(1)(0).inPacket(0).bits.destAddressY, 1)
//poke(c.io.intoFabric(1)(0).inPacket(0).bits.payloadSize, 3)
poke(c.io.intoFabric(1)(0).inPacket(0).bits.payloadSize, 1)
//poke(c.io.intoFabric(1)(0).inPacket(0).bits.payload, 11130952)
poke(c.io.intoFabric(1)(0).inPacket(0).bits.payload, 10)
	for(i <- 0 until COLS) {
		for(j <- 0 until ROWS) {
			poke(c.io.intoFabric(i)(j).inPacket(0).valid, 0)
			poke(c.io.intoFabric(i)(j).inPacket(1).valid, 0)
			poke(c.io.fromFabric(i)(j).outPacket(0).ready, 1)
		}
	}	
	poke(c.io.intoFabric(1)(0).inPacket(0).valid, 1)
    step(1)
    poke(c.io.intoFabric(1)(0).inPacket(0).bits.payload, 20)
	while(t < 20) {
		step(1)
		for(i <- 0 until COLS) {
			for(j <- 0 until ROWS) {
				peek(c.io.fromFabric(i)(j).outPacket(0).bits)
				peek(c.io.fromFabric(i)(j).outPacket(0).valid)
			}
		}
	}

    */
/*	peek(c.io.eastIn.bits.v)
	peek(c.io.eastIn.valid)
	peek(c.io.eastOut.bits.v)
	peek(c.io.eastOut.valid)
	peek(c.io.westIn.bits.v)
	peek(c.io.westIn.valid)
	peek(c.io.westOut.bits.v)
	peek(c.io.westOut.valid)
	peek(c.io.northIn.bits.v)
	peek(c.io.northIn.valid)
	peek(c.io.northOut.bits.v)
	peek(c.io.northOut.valid)
	peek(c.io.southIn.bits.v)
	peek(c.io.southIn.valid)
	peek(c.io.southOut.bits.v)
	peek(c.io.southOut.valid)
	peek(c.io.ejectIn.bits.v)
	peek(c.io.ejectIn.valid)
	peek(c.io.ejectOut.bits.v)
	peek(c.io.ejectOut.valid)
	for(i <- 0 until 50) {
		step(1)
		peek(c.io.eastIn.bits.v)
		peek(c.io.eastIn.valid)
		peek(c.io.eastOut.bits.v)
		peek(c.io.eastOut.valid)
		peek(c.io.westIn.bits.v)
		peek(c.io.westIn.valid)
		peek(c.io.westOut.bits.v)
		peek(c.io.westOut.valid)
		peek(c.io.northIn.bits.v)
		peek(c.io.northIn.valid)
		peek(c.io.northOut.bits.v)
		peek(c.io.northOut.valid)
		peek(c.io.southIn.bits.v)
		peek(c.io.southIn.valid)
		peek(c.io.southOut.bits.v)
		peek(c.io.southOut.valid)
		peek(c.io.ejectIn.bits.v)
		peek(c.io.ejectIn.valid)
		peek(c.io.ejectOut.bits.v)
		peek(c.io.ejectOut.valid)*/
	}

