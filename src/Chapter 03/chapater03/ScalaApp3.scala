package chapater03

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext.doubleRDDToDoubleRDDFunctions
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.util.StatCounter

/**
 * A simple Spark app in Scala
 */
object ScalaApp3 {

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    val user_data = sc.textFile("ml-100k/u.user")
    val user_first = user_data.first()
    //�û�ID|����  |�Ա�|   ְҵ         | �ʱ�
    //1     |24  |  M |technician| 85711
    //ͳ���û�,�Ա�,ְҵ���ʱ����Ŀ
    val usre_files = user_data.map(line => line.split('|')) //�����߷ָ�
    println(usre_files)
    //ͳ���û���
    val num_users = usre_files.map(line => line.head).count()
    //ͳ���Ա����Ŀ
    val num_genders = usre_files.map { line => line(2) }.distinct().count()
    //ͳ��ְҵ����Ŀ
    val num_occuptions = usre_files.map { line => line(3) }.distinct().count()
    //ͳ���ʱ����Ŀ
    val num_zipCode = usre_files.map { line => line(4) }.distinct().count()
    //println("Most popular product: %s with %d purchases".format(mostPopular._1, mostPopular._2))
    println("Users: %d,genders: %d,occupations: %d,ZIP Codes: %d".format(num_users, num_genders, num_occuptions, num_zipCode))
    //��Ӱ����
    val move_data = sc.textFile("ml-100k/u.item")
    val move_nums = move_data.count()
    println(move_nums)
    val move_first = move_data.first()
    //��ӰID|��Ӱ����                       |����ʱ��          |
    //1     |Toy Story (1995)|01-Jan-1995||http://us.imdb.com/M/title-exact?Toy%20Story%20(1995)|0|0|0|1|1|1|0|0|0|0|0|0|0|0|0|0|0|0|0
    println(move_first)
    val move_files = move_data.map(line => line.split('|')) //�����߷ָ�

    //��������
    val rating_data = sc.textFile("ml-100k/u.data")
    val rating_nums = rating_data.count()
    val rating_first = rating_data.first()
    //�û�ID  | ӰƬID   | �Ǽ�   | ʱ���	
    //196	    | 242	    |  3   |	881250949
    println("rating_first:" + rating_first)
    //println(rating_nums)
    val rating_files = rating_data.map(line=>line.split("\t")) //�����߷ָ�
    val ratings = rating_files.map { line =>line(2).toInt } //ע��ת��Int����
    // println(1.max(3))
    val max_rating = ratings.reduce((x, y) => x.max(y)) //���ֵ����
    val min_rating = ratings.reduce((x, y) => x.min(y)) //��Сֵ����
    val mean_rating = ratings.reduce(_ + _)/rating_nums//ƽ������
    //val median_rating=ratings.collect()
  
    val median_per_user = rating_nums / num_users //
    val retinags_per_move = rating_nums / move_nums //
    //
    val stats: StatCounter = ratings.stats()
    println("���ֵ:"+stats.max+"\t��Сֵ:"+stats.min+"\t�м�ֵ:"+stats.mean+"\t������:"+stats.count+"\t�ϼ�ֵ:"+stats.sum+"\t��׼ƫ��:"+stats.stdev)
      println("ͳ������:"+ratings.stats())
    println("Min rating: %d,Max rating: %d,Average rating:%d,Median rating: %d".format(min_rating, max_rating, mean_rating, median_per_user))
    //rating_data ����û�IDΪ����,����Ϊֵ�ļ�ֵ��
    val user_ratings_grouped=rating_data.map{file=>(file(0).toInt,file(2).toInt)}.groupByKey()
    //���ÿ������(�û�ID)��Ӧ���������ϵĴ�С,���������û������Ĵ���
   // val user_ratings_byuser=user_ratings_grouped.map((k,v) => (k,count(v)))
 
    
  }

}
