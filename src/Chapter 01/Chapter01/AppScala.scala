package Chapter01

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object AppScala {
  def main(args: Array[String]) {
   //val sparkConf = new SparkConf().setMast("local[2]").setAppName("SparkHdfsLR")
    
    
    val conf = new SparkConf().setAppName("test").setMaster("local")
    val  sc = new SparkContext(conf)
   // val sc = new SparkContext("local[2]", "First Spark App")
    // we take the raw data in CSV format and convert it into a set of records of the form (user, product, price)
    //��csv��ʽ��ԭʼ����ת��Ϊ(user, product, price),������Ԫ����(�ͻ�����,��Ʒ����,��Ʒ�۸�)
    
    /**
     * ���ݸ�ʽ:
     * �ͻ�		   ��Ʒ����							�۸�
     * John	   iPhone Cover	        9.99
		 * John	   Headphones	          5.49
		 * Jack	   iPhone Cover	        9.99
		 * Jill	   Samsung Galaxy Cover	8.95
		 * Bob	   iPad Cover	          5.49
     */
      
    val data = sc.textFile("data/UserPurchaseHistory.csv")
      .map(line => line.split(","))
      .map(purchaseRecord => (purchaseRecord(0), purchaseRecord(1), purchaseRecord(2)))
    // let's count the number of purchases
    //������������
    val numPurchases = data.count()
    // let's count how many unique users made purchases
    //�ж��ٸ���ͬ�ͻ��������Ʒ
    val uniqueUsers = data.map { case (user, product, price) => {
      println(user)
      user     
     }
    }.distinct().count()
    // let's sum up our total revenue
    //�ó�������
    val totalRevenue = data.map { case (user, product, price) => price.toDouble }.sum()
    // let's find our most popular product
    //������Ĳ�Ʒ��ʲô,
    val productsByPopularity = data
      .map { case (user, product, price) => (product, 1) }.sortByKey(false)
      .reduceByKey(_ + _)  // .sortByKey(false)(����)���� ͬʱ����ʹ��sortByKey()��������в��в�������
      .collect()
     // .sortBy(-_._2)//-��ʾ���� ���Ź����������(����)����,��������
    val mostPopular = productsByPopularity(0)//
    // finally, print everything out
    //5�ν�����Ϣ
    println("Total purchases: " + numPurchases)
     //���пͻ�
    println("Unique users: " + uniqueUsers)
    //������
    println("Total revenue: " + totalRevenue)
    //����Ĳ�Ʒ,���������
    println("Most popular product: %s with %d purchases".format(mostPopular._1, mostPopular._2))

    sc.stop()
  }
}