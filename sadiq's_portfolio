<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Document</title>
    <link rel="stylesheet" href="style.css">
    <link rel="stylesheet" media="screen and (max-width:530px)" href="phone.css">
</head>
<style>
    *{
        margin: 0;
        padding: 0;
        box-sizing: border-box;
    }
    html{
        scroll-behavior: smooth;
    }
    /*navbar */
    .navbar{
        display: flex;
        justify-content: center;
        align-items: center;
        background-color: rgb(0, 0,0,0.5);
        position: sticky;
        top: 0;
    }
    .navbar ul{
        display: flex;
        list-style: none;
        margin: 20px 0px;
    }
    .navbar ui li{
        font-family: century;
        font-size: 1.1rem;
        font-weight: bold;
        font-size: x-large;
    }
    .navbar ul li a{
        text-decoration:maroon;
        color:black;
        padding: 8px 25px;
        transition: all .5s ease;
    }
    .navbar ul li a:hover{
        background-color: rgb(255, 255, 255);
        color: black;
        box-shadow: 0 0 10px white;
    }

    /* Home section */
    #home{
        display: flex;
        flex-direction: column;
        background-color: none;
        height: 1000PX;
        justify-content:center;
        align-items:center;
        color:maroon;
        font-size: x-large;
    }
    #home::before{
        content: "";
        position: absolute;
        top: 0;
        right: 0;
        background:url('https://res.cloudinary.com/dsd8yegd2/image/upload/v1648454033/WhatsApp_Image_2022-03-28_at_1.19.13_PM_afrusn.jpg') no-repeat center center/cover;
        height: 1000px;
        width: 100%;
        z-index: -1;
        opacity: .8;
    }
    .main{
        display: flex;
        flex-direction: column;
        border: 1px solid white;
        align-items: center;
        position: absolute;
        top: 30%;
        right: 10%;
    }
    .headings{
        font-family: century;
        font-size: 3rem;
        text-align:center;
        margin: 40px 0px;
    }
    .btn{
        padding: 10x 35px;
        background-color: transparent;
        border: 1px solid white;
        color:black;
        outline: none;
        transition: .6s ease;
    }
    .btn:hover{
        cursor: pointer;
        background-color: white;
        color: black;
        box-shadow: 0 0 5px white,0 0 10px white,0 0 15px white;
        font-weight: bold;
    }

    /*About Selection */
    about{
        display: flex;
        flex-direction: column;
        box-sizing: border-box;
        padding: 20px;
        margin-bottom: 50px;
    }
    #pic{
        display: flex;
    }
    #pic img{
        width: 575px;
        height: 400px;
    }
    #intro{
        display: flex;
        flex-direction: column;
        text-align: justify;
        padding: 10px;
    }
    #intro h2{
        font-size: 2rem;
        margin-bottom: 20px;
    }

    /* Picture Section */
    #pictures{
        display: flex;
        flex-direction: column;
        background-color: rgba(0, 0, 0, 0.9);
        color: white;
        align-items: center;
        padding: 20px;

    }
    .gallery{
        display: flex;
        flex-wrap: wrap;
        justify-content: space-around;
        box-sizing: border-box;
    }
    .gallery img{
        width: 200px;
        height: 240px;
        margin: 10px;
    }

    /* Skills Section */
    #skills{
        display: flex;
        flex-direction: column;
        padding: 20px;
    }
    .row{
        display: flex;
    }
    .box{
        display: flex;
        flex-direction: column;
        width: 350px;
        height: 450px;
        border: 1px solid black;
        margin: 10px;
        align-items: center;
        text-align: justify;
        padding: 10px;
        border-radius: 15px;
        background: linear-gradient(to top, rgb(76, 45, 255) 50%, white 50%);
        background-size: 100% 200%;
        transition: all .8s;
    }
    .box:hover{
        background-position: left bottom;
        color: wheat;
        border: none;
        box-shadow: 0 0 20px blue;
    }
    .box image{
        width: 80px;
        height: 80px;
        background-color: white;
        padding: 10px;
    }

    /* contact section */
    #contact{
        display: flex;
        flex-direction: column;
        box-sizing: border-box;
        background-color:black;
        color: white;
    }
    .form{
        display: flex;
        flex-direction: column;
        box-sizing: border-box;
        align-items: center;
        margin: 20px 0px;
    }
    .input{
        padding: 12px;
        margin: 15px;
        width: 30%;
        border:none;
        outline: none;
    }
    footer p {
        text-align: center;
    }
    
    .experience-area h1 {
        padding: 5%;
    }
    
    .site-footer
    {
        background-color:black;
        padding:45px 0 20px;
        font-size:15px;
        line-height:24px;
        color:#737373;
    }
    .site-footer hr
    {
        border-top-color:#bbb;
        opacity:0.5
    }
    .site-footer hr.small
    {
        margin:20px 0
    }
    .site-footer h6
    {
        color:#fff;
        font-size:16px;
        text-transform:uppercase;
        margin-top:5px;
        letter-spacing:2px
    }
    .site-footer a
    {
        color:#737373;
    }
    .site-footer a:hover
    {
        color:#3366cc;
        text-decoration:none;
    }
    .footer-links
    {
        padding-left:0;
        list-style:none
    }
    .footer-links li
    {
        display:block
    }
    .footer-links a
    {
        color:#737373
    }
    .footer-links a:active,.footer-links a:focus,.footer-links a:hover
    {
        color:#3366cc;
        text-decoration:none;
    }
    .footer-links.inline li
    {
        display:inline-block
    }
    .site-footer .social-icons
    {
        text-align:center
    }
    .site-footer .social-icons a
    {
        width:60px;
        height:52px;
        line-height:40px;
        border-radius:100%;
        background-color:#33353d
    }
    .copyright-text
    {
        margin:0
    }
    @media (max-width:991px)
    {
        .site-footer [class^=col-]
        {
        margin-bottom:30px
        }
    }
    @media (max-width:767px)
    {
        .site-footer
        {
        padding-bottom:0
        }
        .site-footer .copyright-text,.site-footer .social-icons
        {
        text-align:center
        }
    }
    .social-icons
    {
        padding-left:0;
        margin-bottom:0;
        list-style:none
    }
    .social-icons li
    {
        display:inline-block;
        margin-bottom:5px
    }
    .social-icons li.title
    {
        margin-right:15px;
        text-transform:uppercase;
        color:#96a2b2;
        font-weight:700;
        font-size:13px
    }
    .social-icons a{
        position:relative;
        background-color:#eceeef;
        color:#818a91;
        font-size:30px;
        display:inline-block;
        bottom: 20px;
        line-height:44px;
        width:30px;
        height:34px;
        text-align:center;
        margin-right:8px;
        border-radius:100%;
        -webkit-transition:all .2s linear;
        -o-transition:all .2s linear;
        transition:all .2s linear
    }
    .social-icons a:active,.social-icons a:focus,.social-icons a:hover
    {
        color:#fff;
        background-color:#29aafe
    }
    .social-icons.size-sm a
    {
        line-height:34px;
        height:24px;
        width:24px;
        font-size:12px
    }
    .social-icons a.facebook:hover
    {
        background-color:#3b5998
    }
    .social-icons a.twitter:hover
    {
        background-color:#00aced
    }
    .social-icons a.linkedin:hover
    {
        background-color:#007bb6
    }
    .social-icons a.dribbble:hover
    {
        background-color:#5a5758
    }
    @media (max-width:767px)
    {
        .social-icons li.title
        {
        display:block;
        margin-right:0;
        font-weight:600
        }
    }
</style>
<body>
    <nav class="navbar">
        <ul>
            <li><a href="#home">Home</a></li>
            <li><a href="#about">About me</a></li>
            <li><a href="#pictures">Pictures</a></li>
            <li><a href="#Skills">Skills</a></li>
            <li><a href="#contact">Contact Me</a></li>
        </ul>
    </nav>
        
    <section id="home">
            <div class="Main">
                 <h1 class="Headings">HI!! I AM <br>MAHMAD SADIQ </h1>
                 <button class="btn">
                   WELCOME TO MY PORTFOLIO
              </button>
             </div>
         </section> 

        <section id="about">
            <h1 class="headings">ABOUT ME</h1>
            <div id="pic">
                <img src="E:\c\Users\my pc\Desktop\my vids\IMG-20210407-WA0049.jpg" alt="">
                <div id="intro">
                    <h2>SHAIK MAHMAD SADIQ</h2>

                    <h5>HELLO!This is SADIQ,I am from PRAKASAM(DIST),ANDHRA PRADESH.</h5>
                          <P><h4>EDUCATION DETAILS:</h4></P>
                           I am a student pursuing my bachelor's degree more specifically in the field of "COMPUTER SCIENCE AND ENGINEERING"
                           <ol>
                                 <li>
                              10th class: studied in ZPHS ETHAMUKKALA
</li>
                              GRADE     : 10.0
<li>
                              intermediate : studied in SRI SARASWATHI JUNIOR COLLEGE,ONGOLE
</li>
                              MARKS(1000)  : 985
                           </ol>
                       <h3>HOBBIES:</h3>
<li>
                        spending time with family
</li>
<li>
                        Listening music
</li>
<li>
                        watching movies 
</li>
<li>
                        playing cricket,Kabaddi,chess,badminton
</li>
<li>
                        maintaining myself fit 
</li>
<li>
                        I have recently joined in CSI club.I'm Very happy to be a part of CSI Chapter. 
</li>                    
                     
                    </P>
                </div>
            </div>
        </section>

        <section id="pictures">
            <h1 class="headings">PICTURES</h1>
            <div class="gallery">
                <img src="C:\Users\WELCOME\Desktop\IMG-20210209-WA0012-1.jpg" alt="">
                
                
            </div>
        </section>

        <section id="services">
            <h1 class="headings">SKILLS</h1>
            <div class="row">
                <div class="box">
                    <h1 class="headings">MY SKILLS</h1>
                    <ol>
                          <li>
good communication skills
</li>
                          <li>
good technical knowledge
</li>
                          <li>
HTML and CSS 
</li>
                          <li>
MS office
</li>
                          <li>
OOPS in java
</li>
                          <li>
python      
</li>       
                          <li>
I am more intrested to know more things about technology.
</li>
                </ol>       
                    </div>
        </section>

        <section id="contact">
            <h1 class="heading">CONTACT</h1>
            <form action=""class="form">
                <input type="text" name="name" class="input" placeholder="SADIQ-7995942165">
                <input type="email" name="email" class="input" placeholder="mahmadsadiqshaik@gmail.com">

            </form>
        </section>

        <footer>
            <!-- Add icon library -->
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

<!-- Add font awesome icons -->


            <p>MAHMAD SADIQ SHAIK</p>
        </footer>
        <footer class="site-footer" id="contact">
            <div class="container">
                  <ul class="social-icons">
                    <li><a class="instagram" href="https://www.instagram.com/invites/contact/?i=1pitfu9ure28d&utm_content=63j4kxf"><i class="fa fa-facebook"></i></a></li>
                    
                    
                    
                  </ul>
              </div>
            </div>
      </footer>
</body>
</html>
