package chapter04
import org.apache.spark.mllib.recommendation.ALS
import org.apache.spark.mllib.recommendation.Rating
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.util.StatCounter
import org.jblas.DoubleMatrix

/**
 * A simple Spark app in Scala
 * ����Spark�Ƽ�����
 */
object ScalaApp4 {

  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setMaster("local[2]").setAppName("SparkHdfsLR")
    val sc = new SparkContext(sparkConf)
    //��������
    val rawData = sc.textFile("ml-100k/u.data")
    //�û�ID  | ӰƬID   | �Ǽ�   | ʱ���	
    //196	    | 242	    |  3   |	881250949
    /*��ȡǰ�����ֶμ� �û�ID  | ӰƬID   | �Ǽ� */
    val rawRatings = rawData.map(_.split("\t").take(3)) //ȡ��ǰ�����ֶ�,������ʱ����ʱ����Ҫ
    //Rating�����������Ӧ�û�ID,��Ʒ(��ӰƬID),ʵ���Ǽ�
    //map������ԭ��user ID,movie ID,�Ǽ�������ת��Ϊ��Ӧ�Ķ���,�Ӷ�������������������鼯
    val ratings = rawRatings.map { case Array(user, movie, rating) => Rating(user.toInt, movie.toInt, rating.toDouble) }       
    /**===================��������ģ���Ƽ�=====================================**/
    //ratings.first()
    //����ѵ���Ƽ�ģ��
    //����˵��:
    //rank:��ӦALSģ�����ڵͽ׽��ƾ����е�������������,ͨ������ȡֵΪ10---200
    //iterations:��Ӧ����ʱ��������,10������һ���ͦ��
    //lambda:�ò�������ģ�͵����򻯹���,�Ӷ�����ģ�͵Ĺ�������0.01,�������Ӧ��ͨ���÷������Ĳ������ݽ��н�����֤����     
    val model = ALS.train(ratings, 50, 10, 0.01) //����MatrFactorizationModel�������ֽ�ģ��
    model.userFeatures //�û�����
    model.userFeatures.count
    model.productFeatures.count //��Ʒ���� 
    /**=================ʹ���Ƽ�ģ��==============================**/
    //��ģ��Ԥ���û�789�Ե�Ӱ123������Ϊ3.12 
    //ʹ���Ƽ�ģ��Ԥ���û�����Ʒ�������֣��õ�Ԥ�����ֵ����ݼ�  
    //predict���������������µ����ݵ�����ݵ���ɵ�RDDӦ�ø�ģ�ͽ���Ԥ�⡣
    
    val predictedRating = model.predict(789, 123) //��������û��Ը�����Ʒ��Ԥ�ڵ÷�
    //ÿһ�Զ����ɵ÷�
    val predictedRatings = model.predict(ratings.map(x => (x.user, x.product))) //ÿ���û������в�ƷԤ���
    //�ڶ��ַ�ʽ��RDD
    val userPros = rawRatings.map { case Array(user, movie, rating) => (user.toInt, movie.toInt) }   
    val userProsRDD = model.predict(userPros) //ÿ���û������в�ƷԤ���
    for (pre <- userProsRDD) {
      println("userProsRDD>>>" + pre)
    }
   
    println("Ԥ���û�789�Ե�Ӱ123������Ϊ:" + predictedRating)
    val userId = 789
    val K = 10
    //���¸��û�789�Ƽ���ǰ10����Ʒ 
    val topKRecs = model.recommendProducts(userId, K) //������ģ���û�ID,num����Ҫ�Ƽ�����Ʒ���� 

    println("�û�789�Ƽ���ǰ10����Ʒ\t" + topKRecs.mkString("\n"))

    /*****�����Ƽ�����*******/
    val movies = sc.textFile("ml-100k/u.item")
    //��ӰID|��Ӱ����                       |����ʱ��          |
    //1     |Toy Story (1995)|01-Jan-1995||http://us.imdb.com/M/title-exact?Toy%20Story%20(1995)|0|0|0|1|1|1|0|0|0|0|0|0|0|0|0|0|0|0|0

    //��ȡǰ�������� ,�ӵ�ӰID�ͱ���,����Map��ʽ
    val titles = movies.map(line => line.split("\\|").take(2)).map(array => (array(0).toInt, array(1))).collectAsMap()
    topKRecs.foreach { x => println("ǰ10����Ʒ����:" + titles(x.product.intValue()) + "\t����:" + x.rating.doubleValue()) }
    println("��ӰID 123����:" + titles(123))
    //�ҳ��û�789�������۹��ĵ�Ӱ
    val moviesForUser = ratings.keyBy(_.user).lookup(789)
    //�鿴����û������˶��ٵ�Ӱ
    println("�û�789�����˶��ٵ�Ӱ:" + moviesForUser.size)
    //�û�789�Ե�Ӱ�����������н�������,�������������ǰ10����Ӱ������
    moviesForUser.sortBy(-_.rating).take(10).map(rating => (titles(rating.product), rating.rating)).foreach(println)
    //��һ��ǰ10���Ƽ�
    println("=========��һ��ǰ10���Ƽ�=============")
    topKRecs.map(rating => (titles(rating.product), rating.rating)).foreach(println)

    /***********��Ʒ�Ƽ�ģ��Ч��*******************/
    //��MovieLens 100K���ݼ��������Ƶ�Ӱ
    val aMatrix = new DoubleMatrix(Array(1.0, 2.0, 3.0))
    //����һ��������������������֮����������ƶ�,
    def cosineSimilarity(vec1: DoubleMatrix, vec2: DoubleMatrix): Double = {
      //�������ƶ�:���������ĵ��������������ĳ˻�����,���ƶȵ�ȡֵ��-1��1֮��
      //���ƶ�ȡֵ��-1��1֮��,1��ʾ�� ȫ����,0��ʾ���߲����(����������)
      val retur=vec1.dot(vec2) / (vec1.norm2() * vec2.norm2())
      retur
    }
    //����Ʒ567Ϊ����ģ����ȡ�����Ӧ������,
    val itemId = 567
    //���ص�һ�����������ֻ���һ��ֵ(ʵ����,������Ҳֻ����һ��ֵ,Ҳ���Ǹ���Ʒ����������)
    val itemFactor = model.productFeatures.lookup(itemId).head
    
    //����һ��DoubleMatrix����,Ȼ�����øö��������������Լ������ƶ�
    val itemVector = new DoubleMatrix(itemFactor)
    cosineSimilarity(itemVector, itemVector)
    //�����������Ʒ���������ƶ�
    val sims = model.productFeatures.map {
      case (id, factor) =>
        val factorVector = new DoubleMatrix(factor)
        //��������
        val sim = cosineSimilarity(factorVector, itemVector)
        (id, sim)
    }
    //����Ʒ�������ƶ�����,Ȼ��ȡ������Ʒ567�����Ƶ�ǰ10����Ʒ, 
    //top����Ordering����,�������Spark���ݼ�ֵ�����ֵ����(Ҳ������similarity����)
    val sortedSims = sims.top(K)(Ordering.by[(Int, Double), Double] { case (id, similarity) => similarity })
    println(sortedSims.mkString("\n"))
    /* 
    (567,1.0000000000000002)
    (413,0.7431091494962073)
    (1471,0.7301420755366541)
    (288,0.7229307761535951)
    (201,0.7174894985405192)
    (895,0.7167458613072292)
    (403,0.7129522148795585)
    (219,0.712221807408798)
    (670,0.7118086627261311)
    (853,0.7014903255601453)
		*/
    /**����Ƽ���������Ʒ**/
    //����Ʒ���ƶ�����,Ȼ��ȡ������Ʒ567�����Ƶ�ǰ10����Ʒ
    //����Ordering����,�������Spark���ݼ�ֵ�����ֵ����(Ҳ������similarity����)
    println(titles(itemId)) //Wes Craven's New Nightmare (1994)
    val sortedSims2 = sims.top(K + 1)(Ordering.by[(Int, Double), Double] { case (id, similarity) => similarity })
    //slice��ȡ��1�п�ʼ��11�е���������
    sortedSims2.slice(1, 11).map { case (id, sim) => (titles(id), sim) }.mkString("\n" + ">>>>>")
    /* 
    (Hideaway (1995),0.6932331537649621)
    (Body Snatchers (1993),0.6898690594544726)
    (Evil Dead II (1987),0.6897964975027041)
    (Alien: Resurrection (1997),0.6891221044611473)
    (Stephen King's The Langoliers (1995),0.6864214133620066)
    (Liar Liar (1997),0.6812075443259535)
    (Tales from the Crypt Presents: Bordello of Blood (1996),0.6754663844488256)
    (Army of Darkness (1993),0.6702643811753909)
    (Mystery Science Theater 3000: The Movie (1996),0.6594872765176396)
    (Scream (1996),0.6538249646863378)
    */

    /**�Ƽ�ģ��Ч��������***/
    //�û�789�ҳ���һ������
    val actualRating = moviesForUser.take(1)(0) //
    //Ȼ����ģ�͵�Ԥ������
    val predictedRatingR = model.predict(789, actualRating.product)
    //����ʵ��������Ԥ��������ƽ�����
    val squaredError = math.pow(predictedRatingR - actualRating.rating, 2.0)
    //���ȴ�ratings��ȡ�û�����ƷID
    val usersProducts = ratings.map { case Rating(user, product, rating) => (user, product) }
    //�Ը���"�û�-��Ʒ"����Ԥ��,���õ�RDD��"�û�����ƷID"����Ϊ����,��Ӧ��Ԥ��������Ϊֵ
    val predictions = model.predict(usersProducts).map {
      case Rating(user, product, rating) => ((user, product), rating)
    }
    //�����ʵ������,ͬʱ��ratingsRDD��ӳ������"�û�-��Ʒ"��Ϊ����,ʵ������Ϊ��Ӧ��ֵ,�͵õ�����������֯��ͬ��RDD,
    //��������������,�Դ���һ���µ�RDD
    val ratingsAndPredictions = ratings.map {
      case Rating(user, product, rating) => ((user, product), rating)
    }.join(predictions)
    //������==��Ҫ��ÿһ����¼�������ƽ�����,Ȼ�����,�ٳ����ܵ���������
    val MSE = ratingsAndPredictions.map {
      //����ƽ�����ĺ�������Ŀ����,����ƽ�������ָԤ�⵽����������ʵ�����Ĳ�ֵ�Ĵη�
      //actualʵ��ֵ,predictedԤ��ֵ,x��2�η�
      case ((user, product), (actual, predicted)) => math.pow((actual - predicted), 2)
    }.reduce(_ + _) / ratingsAndPredictions.count
    //
    println("������:Mean Squared Error = " + MSE)
    //��ͬ����Ԥ��������ʵ�������Ĳ�ֵ�ı�׼��
    val RMSE = math.sqrt(MSE) //���������
    println("Root Mean Squared Error = " + RMSE)
    val actualMovies = moviesForUser.map(_.product)//
    val predictedMovies = topKRecs.map(_.product)//
    val apk10 = avgPrecisionK(actualMovies, predictedMovies, 10)  
    //ȡ����Ʒ������������������һ��DoubleMatrix
    val itemFactors = model.productFeatures.map { case (id, factor) => factor }.collect()
    val itemMatrix = new DoubleMatrix(itemFactors)
    //��ӡ�������ֱ�Ϊ1682��50,��Ϊ��Ӱ��Ŀ������ά��������ô��
    println(itemMatrix.rows, itemMatrix.columns)
    //���þ�����һ���㲥�����ķ�ʽ�ַ���ȥ,�Ա�ÿ�������ڵ㶼�ܷ��ʵ�
    val imBroadcast = sc.broadcast(itemMatrix)
    
    val allRecsQ = model.userFeatures.map {//
      case (userId, array) =>
        val userVector = new DoubleMatrix(array) //��Ʒ���� array
        val scores = imBroadcast.value.mmul(userVector)//�������
        val sortedWithId = scores.data.zipWithIndex.sortBy(-_._1) //��ӰID����Ԥ�������ĸߵ�ת��,ת��K,����ֵ��ֵ��
        val recommendedIds = sortedWithId.map(_._2 + 1).toSeq //����ƷID��1,��Ϊ��Ʒ���Ӿ���ı�Ŵ�0��ʼ,�����ǵ�Ӱ�ı�Ŵ�1��ʼ
        (userId, recommendedIds)
    }  
    //�����������û��������Ʒ,����ÿ���û�ID����Ӧ��(userId,movieId)��,��Ϊgroup������ʹ�õ����������û�ID
    val userMovie = ratings.map { case Rating(user, product, rating) => (user, product) }.groupBy(_._1)    
    //ʹ��avgPrecisionK�����������
    //Join�������������RDD���û�ID������,����ÿһ���û�,���Ƕ���һ��ʵ�ʺ�Ԥ�����Щ��Ӱ��ID
    //��ЩID������ΪAPK����������,�ڼ���MSEʱ����,���ǵ���reduce����������ЩAPK�÷����,Ȼ�������ܵ��û���Ŀ
    //(��allRecs RDD�Ĵ�С)
    val MAPK = allRecsQ.join(userMovie).map {
      case (userId, (predicted, actualWithIds)) =>
        val actual = actualWithIds.map(_._2).toSeq
        avgPrecisionK(actual, predicted, K)
    }.reduce(_ + _) / allRecsQ.count
    println("Mean Average Precision = " + MAPK)
    // Mean Average Precision = 0.07171412913757186
    

    /**MLib���õ���������**/
    // MSE, RMSE and MAE
    import org.apache.spark.mllib.evaluation.RegressionMetrics
    /**
     * ��׼���(Standard error)Ϊ������ֵ����ƽ���͵�ƽ��ֵ��ƽ����,
     * ��Ҳ�ƾ��������(Root mean squared error)������ͬ���������½��еĲ�����Ϊ�Ⱦ��Ȳ���
     */ 
       //����һ��Ԥ��ֵ��ʵ��ֵ
    val predictedAndTrue = ratingsAndPredictions.map { case ((user, product), (actual, predicted)) => (actual, predicted) }
    //ʵ����һ��RegressionMetrics������Ҫһ����ֵ�����͵�RDD,��ÿһ����¼��Ӧÿ�����ݵ�����Ӧ��Ԥ��ֵ��ʵ��ֵ
    val regressionMetrics = new RegressionMetrics(predictedAndTrue)
    //������
    println("Mean Squared Error = " + regressionMetrics.meanSquaredError)
    //���������
    println("Root Mean Squared Error = " + regressionMetrics.rootMeanSquaredError)
    // Mean Squared Error = 0.08231947642632852
    // Root Mean Squared Error = 0.2869137090247319

    // MAPK ׼ȷ��
    //RankingMetrics�����������������������ָ��
    import org.apache.spark.mllib.evaluation.RankingMetrics
     //�����������û��������Ʒ,����ÿ���û�ID����Ӧ��(userId,movieId)��,��Ϊgroup������ʹ�õ����������û�ID
    val userMovies = ratings.map { case Rating(user, product, rating) => (user, product) }.groupBy(_._1)
    //�û����ӽ���һ��map����,����û����Ӿ���͵�Ӱ���Ӿ������˻�,����Ϊһ����ʾ������ӰԤ������������(����1682����Ӱ������Ŀ)
    //֮��,��Ԥ����������������
    val allRecs = model.userFeatures.map {//
      case (userId, array) =>
        val userVector = new DoubleMatrix(array) //��Ʒ���� array
        val scores = imBroadcast.value.mmul(userVector)//�������
        val sortedWithId = scores.data.zipWithIndex.sortBy(-_._1) //��ӰID����Ԥ�������ĸߵ�ת��,ת��K,����ֵ��ֵ��
        val recommendedIds = sortedWithId.map(_._2 + 1).toSeq //����ƷID��1,��Ϊ��Ʒ���Ӿ���ı�Ŵ�0��ʼ,�����ǵ�Ӱ�ı�Ŵ�1��ʼ
        (userId, recommendedIds)
    }
    //��Ҫ������֮ǰ��ƽ��׼ȷ�ʺ�������һ����ֵ�����͵�RDD,
    //���Ϊ�����û�Ԥ�����Ʒ��ID����,��ֵ����ʵ�ʵ���ƷID����
       val predictedAndTrueForRanking = allRecs.join(userMovies).map {
      case (userId, (predicted, actualWithIds)) =>
        val actual = actualWithIds.map(_._2)
        (predicted.toArray, actual.toArray)
    }
    //ʹ��RankingMetrics������������������ָ��,��Ҫ������֮ǰƽ���ʺ�������һ����ֵ�����͵�RDD
    //���Ϊ�����û�Ԥ����Ƽ���Ʒ��ID����,��ֵ��ʵ�ʵ���ƷID����
    val rankingMetrics = new RankingMetrics(predictedAndTrueForRanking)
    //Kֵƽ��׼ȷ��meanAveragePrecision
    println("ƽ��׼ȷ��:Mean Average Precision = " + rankingMetrics.meanAveragePrecision)
    // Mean Average Precision = 0.07171412913757183

    // Compare to our implementation, using K = 2000 to approximate the overall MAP
    //ʹ��avgPrecisionK�����������
    //Join�������������RDD���û�ID������,����ÿһ���û�,���Ƕ���һ��ʵ�ʺ�Ԥ�����Щ��Ӱ��ID
    //��ЩID������ΪAPK����������,�ڼ���MSEʱ����,���ǵ���reduce����������ЩAPK�÷����,Ȼ�������ܵ��û���Ŀ
    //(��allRecs RDD�Ĵ�С)
    val MAPK2000 = allRecs.join(userMovies).map {
      case (userId, (predicted, actualWithIds)) =>
        val actual = actualWithIds.map(_._2).toSeq
        avgPrecisionK(actual, predicted, 2000)
    }.reduce(_ + _) / allRecs.count
    //���ָ��Kֵʱ��ƽ��׼ȷ��
    println("Mean Average Precision = " + MAPK2000)
    // Mean Average Precision = 0.07171412913757186
  }
  //���ָ��Kֵʱ��ƽ��׼ȷ��,APK�����ں������ĳ����ѯ���ص�"ǰK��"�ĵ���ƽ�������
  //���������ĵ���ʵ�������Խ��������Ҳ����ǰ,��APK��ֵҲ��Խ��,�ʺ������ĺû�,��Ϊ�Ƽ�ϵͳ
  //Ҳ�����ǰK���Ƽ���,Ȼ����ָ��û�,APK����������������ָ��ͬ���ʺ�������ʽ���ݼ��ϵ��Ƽ�
  //������MSE��ԾͲ���ô�ʺ�,APKƽ��׼ȷ��
  def avgPrecisionK(actual: Seq[Int], predicted: Seq[Int], k: Int): Double = {
    val predK = predicted.take(k)
    var score = 0.0
    var numHits = 0.0
    //zipWithIndex ��RDD�е�Ԫ�غ����Ԫ����RDD�е�ID(������)��ϳɼ�/ֵ��
    val zip=predK.zipWithIndex
    //pֵ,i������
    for ((p, i) <- zip) {
      //println("p:"+p+"=="+i)
      if (actual.contains(p)) {
        numHits += 1.0
        score += numHits / (i.toDouble + 1.0)
      }
    }
    if (actual.isEmpty) {
      1.0
    } else {
      score / scala.math.min(actual.size, k).toDouble
    }
  }

}
