import java.io.PrintWriter
import java.net.ServerSocket
import java.text.{SimpleDateFormat, DateFormat}
import java.util.Date

import org.apache.spark.SparkContext._
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.util.Random

/**
 * A producer application that generates random "product events", up to 5 per second, and sends them over a
 * network connection
 */
object StreamingProducer {

  def main(args: Array[String]) {

    val random = new Random()

    // Maximum number of events per second
    //ÿ���¼����������
    val MaxEvents = 6

    // Read the list of possible names
    //��ȡ�������Ƶ��б�
    val namesResource = this.getClass.getResourceAsStream("/names.csv")
    val names = scala.io.Source.fromInputStream(namesResource)
      .getLines()//�ж�ȡ
      .toList//ת���б�
      .head//ȡ����һ��
      .split(",")//�ָ���������
      .toSeq

    // Generate a sequence of possible products
    //���ɿ��ܵĲ�Ʒ����
    val products = Seq(
      "iPhone Cover" -> 9.99,
      "Headphones" -> 5.49,
      "Samsung Galaxy Cover" -> 8.95,
      "iPad Cover" -> 7.49
    )

    /** 
     *  Generate a number of random product events 
     *  �������������Ʒ�¼�
     *  */
    def generateProductEvents(n: Int) = {
      (1 to n).map { i =>
        //�������һ����Ʒ�ͼ۸�
        val (product, price) = products(random.nextInt(products.size))
        //����Seq�������shuffle
        val user = random.shuffle(names).head
        (user, product, price)
      }
    }

    // create a network producer
    //����һ���������,�����˿�9999
    val listener = new ServerSocket(9999)
    println("Listening on port: 9999")

    while (true) {
      //���ܼ���
      val socket = listener.accept()
      //�����߳�
      new Thread() {
        override def run = {
          println("Got client connected from: " + socket.getInetAddress)
          //
          val out = new PrintWriter(socket.getOutputStream(), true)

          while (true) {
            Thread.sleep(1000)
            //������������
            val num = random.nextInt(MaxEvents)            
            val productEvents = generateProductEvents(num)
            productEvents.foreach{ event =>
              out.write(event.productIterator.mkString(","))
              out.write("\n")
            }
            out.flush()
            println(s"Created $num events...")
          }
          socket.close()
        }
      }.start()
    }
  }
}

/**
 * A simple Spark Streaming app in Scala
 * �򵥵�Spark Streaming����
 */
object SimpleStreamingApp {

  def main(args: Array[String]) {
   //
    val ssc = new StreamingContext("local[2]", "First Streaming App", Seconds(10))
    val stream = ssc.socketTextStream("localhost", 9999)

    // here we simply print out the first few elements of each batch
    //������,����ֻ���ӡ��ÿ�����ε�ǰ����Ԫ��
    println("==================")
    stream.print()
    ssc.start()
    ssc.awaitTermination()

  }
}

/**
 * A more complex Streaming app, which computes statistics and prints the results for each batch in a DStream
 * ������ӡ
 */
object StreamingAnalyticsApp {

  def main(args: Array[String]) {

    val ssc = new StreamingContext("local[2]", "First Streaming App", Seconds(10))
    val stream = ssc.socketTextStream("localhost", 9999)

    // create stream of events from raw text elements
    //��ԭʼ�ı�Ԫ�ش����¼���
    val events = stream.map { record =>
      val event = record.split(",")
      (event(0), event(1), event(2))
    }

    /*
      We compute and print out stats for each batch.
      Since each batch is an RDD, we call forEeachRDD on the DStream, and apply the usual RDD functions
      we used in Chapter 1.
      foreachRDD����Dstream�е����ݷ��͵��ⲿ���ļ�ϵͳ��,�ⲿ�ļ�ϵͳ��Ҫ�����ݿ�
     */
    events.foreachRDD { (rdd, time) =>
      val numPurchases = rdd.count()//����
      val uniqueUsers = rdd.map { case (user, _, _) => user }.distinct().count()
      val totalRevenue = rdd.map { case (_, _, price) => price.toDouble }.sum()//�ϼ�
      val productsByPopularity = rdd
        .map { case (user, product, price) => (product, 1) }//��Ʒ�ϼ�
        .reduceByKey(_ + _)
        .collect()
        .sortBy(-_._2)//����
      val mostPopular = productsByPopularity(0)

      val formatter = new SimpleDateFormat
      //�����յ�����ʱ��
      val dateStr = formatter.format(new Date(time.milliseconds))
      println(s"== Batch start time: $dateStr ==")
      println("Total purchases: " + numPurchases)//��������
      println("Unique users: " + uniqueUsers)//�����û���
      println("Total revenue: " + totalRevenue)//����
      //������û��Ͳ�Ʒ
      println("Most popular product: %s with %d purchases".format(mostPopular._1, mostPopular._2))
    }

    // start the context
    ssc.start()
    ssc.awaitTermination()

  }

}

object StreamingStateApp {
  import org.apache.spark.streaming.StreamingContext._
  /**
   * ״̬���� 
   */
  def updateState(prices: Seq[(String, Double)], currentTotal: Option[(Int, Double)]) = {
    //ͨ��Spark�ڲ���reduceByKey��key��Լ��Ȼ�����ﴫ��ĳkey��ǰ���ε�Seq/List,�ټ��㵱ǰ���ε��ܺ�
    val currentRevenue = prices.map(_._2).sum
 
    val currentNumberPurchases = prices.size
    //���ۼӵ�ֵ
    val state = currentTotal.getOrElse((0, 0.0))
    Some((currentNumberPurchases + state._1, currentRevenue + state._2))
  }

  def main(args: Array[String]) {

    val ssc = new StreamingContext("local[2]", "First Streaming App", Seconds(10))
    // for stateful operations, we need to set a checkpoint location
    //ʹ��updateStateByKeyǰ��Ҫ����checkpoint
    ssc.checkpoint("/tmp/sparkstreaming/")
    val stream = ssc.socketTextStream("localhost", 9999)

    // create stream of events from raw text elements
    //��ԭʼ�ı�Ԫ�ش����¼���
    val events = stream.map { record =>
      val event = record.split(",")
      (event(0), event(1), event(2).toDouble)
    }

    val users = events.map{ case (user, product, price) => (user, (product, price)) }
    //updateStateByKey�����µ�������Ϣ��������ʱ���������û�������Ҫ���κ�״
    val revenuePerUser = users.updateStateByKey(updateState)
    revenuePerUser.print()

    // start the context
    ssc.start()
    ssc.awaitTermination()

  }
}