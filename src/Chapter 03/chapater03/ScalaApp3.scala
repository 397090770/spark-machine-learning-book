package chapater03

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext.doubleRDDToDoubleRDDFunctions
import org.apache.spark.SparkContext.rddToPairRDDFunctions
import org.apache.spark.util.StatCounter
import breeze.linalg.max
import breeze.linalg.min
import scala.collection.mutable.TreeSet
import scala.collection.immutable.TreeMap
import scala.collection.immutable.HashMap
import scala.collection.mutable

/**
 * A simple Spark app in Scala
 */
object ScalaApp3 {

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    /**�û�����****/
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
    /**��������**/
    val rating_data = sc.textFile("ml-100k/u.data")
    val rating_nums = rating_data.count()
    val rating_first = rating_data.first()
    //�û�ID  | ӰƬID   | �Ǽ�   | ʱ���	
    //196	    | 242	    |  3   |	881250949
    println("rating_first:" + rating_first)
    //println(rating_nums)
    val rating_files = rating_data.map(line => line.split("\t")) //�����߷ָ�
    val ratings = rating_files.map { line => line(2).toInt } //ע��ת��Int����
    // println(1.max(3))
    val max_rating = ratings.reduce((x, y) => x.max(y)) //���ֵ����
    val min_rating = ratings.reduce((x, y) => x.min(y)) //��Сֵ����
    val mean_rating = ratings.reduce(_ + _) / rating_nums //ƽ������
    //val median_rating=ratings.collect()

    val median_per_user = rating_nums / num_users //��������/�û�����
    val retinags_per_move = rating_nums / move_nums //��������/��Ӱ����
    //
    val stats: StatCounter = ratings.stats()
    println("���ֵ:" + stats.max + "\t��Сֵ:" + stats.min + "\t�м�ֵ:" + stats.mean + "\t������:" + stats.count + "\t�ϼ�ֵ:" + stats.sum + "\t��׼ƫ��:" + stats.stdev)
    println("ͳ������:" + ratings.stats())
    println("Min rating: %d,Max rating: %d,Average rating:%d,Median rating: %d".format(min_rating, max_rating, mean_rating, median_per_user))
    //rating_data ����û�IDΪ����,����Ϊֵ�ļ�ֵ��
    val user_ratings_grouped = rating_data.map { file => (file(0).toInt, file(2).toInt) }.groupByKey()
    //���ÿ������(�û�ID)��Ӧ���������ϵĴ�С,���������û������Ĵ���
    // val user_ratings_byuser=user_ratings_grouped.map((k,v) => (k,count(v)))

    /***��Ӱ����***/
    val movie_data = sc.textFile("ml-100k/u.item")
    //��ӰID|��Ӱ����                       |����ʱ��          |
    //1     |Toy Story (1995)|01-Jan-1995||http://us.imdb.com/M/title-exact?Toy%20Story%20(1995)|0|0|0|1|1|1|0|0|0|0|0|0|0|0|0|0|0|0|0
    println(movie_data.first())
    //������1682s
    val num_movies = movie_data.count()
    println("Movies: %d".format(num_movies))
    val movie_fields = movie_data.map(lines => lines.split('|')) //ע���ǵ����� 

    //�ӵ�Ӱ�����л�ȡ�������ֶβ�ȡ����ݲ�ת����ֵ,�����ƥ��Ĭ��Ϊ1900�� 
    val pattern = "([0-9]+)-([A-Za-z]+)-([0-9]+)".r
    val years = movie_fields.map(fields => fields(2)).map(x => {
      x match {
        case pattern(num, item, year) => {
          //println(">>>>>>>>>>>"+year.toInt)
          year.toInt
        }
        case _ => {
          //println(x)
          1900
        }
      }
    })
    // # we filter out any 'bad' data points here
    //���˵�1900
    val years_filtered = years.filter(x => x != 1900)
    val years_1900 = years.filter(x => x == 1900)
    //ʹ��1988���ȥ���,ͬʱ��K����ͳ��,����<K,Long>��ֵ�ԣ�Long��ÿ��K���ֵ�Ƶ��
    //���㲻ͬ�����Ӱ��Ŀ
    val movie_ages = years_filtered.map(yr => 1998 - yr).countByValue()
    //ȡ����Ӱ��Ŀ
    val values = movie_ages.values
    //ȡ����Ӱ����
    val bins = movie_ages.keys

    /**��������**/
    val rating_data_raw = sc.textFile("ml-100k/u.data")
    //�û�ID  | ӰƬID   | ����   | ʱ���	
    //196	    | 242	    |  3   |	881250949
    println(rating_data_raw.first())
    //10W������
    val num_ratings = rating_data_raw.count()
    println("�ȼ�������:" + num_ratings)
    //���ݷָ�
    val rating_dataRaw = rating_data_raw.map(line => line.split("\t"))
    //ȡ���ȼ�����
    val ratingsRaw = rating_dataRaw.map(fields => fields(2).toInt)
    //ȡ�����ȼ�
    val max_ratingRaw = ratingsRaw.reduce((x, y) => max(x, y))
    //ȡ����С�ȼ�
    val min_ratingRaw = ratingsRaw.reduce((x, y) => min(x, y))
    //ƽ���ȼ�3.5
    val mean_ratingRaw = ratingsRaw.reduce((x, y) => x + y) / num_ratings.toFloat
    val statsRaw: StatCounter = ratings.stats()
    //������λ��4
    val median_rating = statsRaw.mean
    //ƽ���û�����
    val ratings_per_user = num_ratings / num_users
    //ƽ����Ӱ����
    val ratings_per_movie = num_ratings / num_movies
    print("Min rating: %d".format(min_rating))
    print("Max rating: %d".format(max_rating))
    print("Average rating: %2.2f".format(mean_rating))
    print("Median rating: %d".format(median_rating))
    print("Average # of ratings per user: %2.2f".format(ratings_per_user))
    print("Average # of ratings per movie: %2.2f".format(ratings_per_movie))

    /**ȱʧ�����������淢������������������**/
    //���˵��������������� 
    val years_pre_processed = movie_fields.map(fields => fields(2)).map(x => {
      x match {
        case pattern(num, item, year) => {
          //println(">>>>>>>>>>>"+year.toInt)
          year.toInt
        }
        case _ => {
          //println(x)
          1900
        }
      }
    }).filter(yr => yr != 1900)

    val years_pre_processed_array: StatCounter = years_pre_processed.stats()

    /**ְҵ**/
    val user_fields = user_data.map(line => line.split('|'))
    //�û�ID|����  |�Ա�|   ְҵ         | �ʱ�
    // 1    |24  |  M |technician| 85711
    val all_occupations = user_fields.map(fields => fields(3)).distinct().collect()
    //����
    all_occupations.sorted
    val map = mutable.Map.empty[String, Int]
    var idx = 0
    for (occu <- all_occupations) {
      idx += 1
      map(occu) = idx
    }
    println("Encoding of 'doctor': %d".format(map("doctor")))
    println("Encoding of 'programmer': %d".format(map("programmer")))

  }

}
