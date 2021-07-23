from os import system,remove,chdir
import matplotlib.pyplot as plt


if __name__ == "__main__":
    x = []
    y1 = []
    y2 = []
    y3 = []
    y4 = []

    system("make")
    system("make clean")  
 
    

    
    fp = open("examples/torus88", "r")
    data = fp.readlines()
    class_data=data[48]
    fp.close()
           
        
    for i in range(1,155):  #0.005 to 0.205
        x.append(float((i*5)/1000))
        fp = open("examples/torus88", "r")
        data = fp.readlines()
        data[40] = f'injection_rate     = {x[-1]};\n'
        fp = open("examples/torus88", "w")
        fp.writelines(data)
        fp.close()
       
        system("./booksim examples/torus88 > temp.txt")
        
        
        df = open("temp.txt")
  
        # read file
        read = df.read()
  
        # return cursor to
        # the beginning
        # of the file.
        df.seek(0)
        read
        
        # create empty list
        arr = []
  
        # count number of
        # lines in the file
        line = 1
        for word in read:
            if word == '\n':
               line += 1

    
        fp = open("temp.txt", "r")
        data = fp.readlines()
        y1.append(float(data[(line)-29].split()[4]))
        y2.append(float(data[(line)-14].split()[5]))
        fp.close()
        
       
        if class_data == "classes = 2;\n":
           f = open("temp.txt", "r")
           data = f.readlines()
           y3.append(float(data[(line)-57].split()[4]))
           y4.append(float(data[(line)-42].split()[5]))
           f.close()
     
           
        
        
          
        
    
    print (x,y1,y2,y3,y4)
    f,ax=plt.subplots(1)
    ax.plot(x, y1,marker='o',markerfacecolor='red',markersize=3,label='class 1')
    if class_data == "classes = 2;\n":
        ax.plot(x, y3,marker='o',markerfacecolor='yellow',markersize=3,label='class 0') 
    plt.xlabel("Injection Rate")
    plt.ylabel("Packet latency average")
    plt.title("Packet_latency average v/s Injection Rate\n(Class:1, Atm.VC.alloc:Low)")
    plt.legend()
    ax.set_ylim(bottom=0)
    ax.set_xlim(left=0)
    ax.yaxis.set_minor_locator(plt.MultipleLocator(0.1))
    ax.xaxis.set_minor_locator(plt.MultipleLocator(0.005))
    plt.savefig("Packet_latency_average_v-s_Injection_Rate.png")
    plt.clf()
    f,ax=plt.subplots(1)
    ax.plot(x, y2,marker='o',markerfacecolor='green',markersize=3,label='class 1')
    if class_data == "classes = 2;\n":    
        ax.plot(x, y4,marker='o',markerfacecolor='pink',markersize=3,label='class 0')
    plt.xlabel("Injection Rate")
    plt.ylabel("Accepted packet rate average")
    plt.title("Accepted packet rate average v/s Injection Rate\n(Class:1, Atm.VC.alloc:Low)")
    plt.legend()
    ax.set_ylim(bottom=0)
    ax.set_xlim(left=0)
    ax.yaxis.set_minor_locator(plt.MultipleLocator(0.001))
    ax.xaxis.set_minor_locator(plt.MultipleLocator(0.005))
    plt.savefig("Accepted_packet_rate_average_v-s_Injection_Rate.png")
    plt.clf()
