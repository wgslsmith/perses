

extern int test(int);
void foo()
{
  int i;

  for (i = 0; i < 10; ++i)
    {
#pragma omp sections
 {
 continue;
      }
    }

#pragma omp sections
 {
#pragma omp section
 { bad1: ; }
#pragma omp section
 goto bad1;
    }

#pragma omp sections
 {
      goto bad2;
    }
  bad2:;

  goto bad3;
#pragma omp sections
 {
      bad3: ;
    }

#pragma omp sections
 {
      {
 goto ok1;
 ok1:;
      }
#pragma omp section
 for (i = 0; i < 10; ++i)
 if (test(i))
   break;
 else
   continue;

#pragma omp section
 switch (i)
 {
 case 0:
   break;
 default:
   test(i);
 }
    }
}
