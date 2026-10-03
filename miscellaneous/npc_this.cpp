#include <iostream>
#include <vector>
#include <chrono>
#include <thread>
#include <algorithm>
#include <cstdlib>
#include <ctime>

const int WDD = 10;
const int HDD = 10;

class Human
{
public:
    int x;
    int y;
    int hngr;
    int power;
    bool isAlive = true;

    Human()
    {
        x = rand() % WDD;
        y = rand() % HDD;

        hngr = 40;
        power = 100;
    }

    void Update(std::vector<std::vector<char>>& grid)
    {
        hngr += 2;
        power -= 1;

        if(hngr > 80) Eat(grid);

        if(power < 30) Rest();

        else Wander();

        hngr = std::clamp(hngr, 0, 120);
        power = std::clamp(power, 0, 100);

        if(hngr >= 120 || power <= 0) isAlive = false;
    }

    void Eat(std::vector<std::vector<char>>& grid)
    {
        if(grid[y][x] == 'F')
        {
            std::cout<<"Human eats food.\n";

            hngr -= 40;
            grid[y][x] = ' ';
        }
        else
        {
            std::cout<<"Hungry... searching for food.\n";

            Move();
        }
    }

    void Rest()
    {
        std::cout<<"Human is resting.\n";

        power += 15;
    }

    void Wander()
    {
        std::cout<<"Human is wandering...\n";

        Move();
    }

    void Move()
    {
        int dir = rand() % 4;

        if(dir == 0 && y > 0) y--;

        else if(dir == 1 && y < HDD - 1) y++;

        else if(dir == 2 && x > 0) x--;

        else if(dir == 3 && x < WDD - 1) x++;
    }
};

void Draw(const std::vector<std::vector<char>>& grid, const Human& h)
{
    for(int i = 0; i < HDD; i++)
    {
        for(int j = 0; j < WDD; j++)
        {
            if(i == h.y && j == h.x)
            {
                std::cout<<'H'<< ' ';
            }
            else
            {
                std::cout<<grid[i][j]<< ' ';
            }
        }

        std::cout << "\n";
    }
}

int main()
{
    srand(time(0));

    std::vector<std::vector<char>> grid(HDD,std::vector<char>(WDD, ' '));

    for(int i = 0; i < 10; i++)
    {
        int fx = rand() % WDD;
        int fy = rand() % HDD;

        grid[fy][fx] = 'F';
    }

    Human h;

    while(true)
    {
        h.Update(grid);

        Draw(grid, h);

        std::cout<<"\nHunger: "<< h.hngr;
        std::cout<<"\nEnergy: "<< h.power;
        std::cout<<"\nAlive: "<< (h.isAlive ? "Yes" : "No") << "\n\n";

        if(!h.isAlive)
        {
            std::cout<<"Human is dead.\n";
            break;
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(550));
    }

    return 0;
}
