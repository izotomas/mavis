# mavis-private

MAvis is short for "Multi-Agent Visualisation Tool". It is a tool to support the design, implementation and test of single- and multi-agent planning algorithms. Various discrete single-and multi-agent domains can be modelled and visualised with the tool. The instances of a given domain are called **levels**, and these are typically what is referred to as **planning problems** or **planning tasks** within the area of automated planning (a given goal has to be reached from a given initial state through a sequence of actions). The tool doesn't provide any planning system for solving levels, it only provides the models of the different domains and a graphical user interface to visualise levels and animate solutions. The system for visualising levels and solutions is called the (**MAvis**) **server**. Users of the tool (e.g. students or researchers in AI) then have to provide their own AI system (planning system) for solving levels. That AI system is called a **client**. 

The original domain developed for MAvis is called the **hospital domain**. It is a multi-agent domain where agents have to move boxes into designated goal cells. Each goal cell has to be matched by a box of the same letter, and each agent can only move a box of the same color as itself. A level can also contain goal cells for the agents, hence making the domain into a generalisation of the multi-agent path finding problem (Stern et al, 2019). The following video shows a client (Croissant) solving a level (MAAIoliMAsh) in the hospital domain: 

https://user-images.githubusercontent.com/11925062/127993622-61a5fb10-ac0f-4a3a-9de8-e314a96c2b67.mov

Both client and level are made by students attending the course _02285 Artificial Intelligence and Multi-Agent Systems_ at the Technical University of Denmark (DTU). The MAvis tool was originally developed to be used as a teaching tool in that particular course, and the original version only supported that single domain. A detailed description of the hospital domain is available in [docs/domains/hospital/hospital_domain.pdf](docs/domains/hospital/hospital_domain.pdf).

Here is another example of the tool visualising an optimal solution to a medieval French labyrinth, _Labyrinth of St. Bertin_, computed by a simple Breadth-First Search (BFS) client (the solution consists of 1110 actions (moves) and was computed in 555ms as can be seen from the top bar of the server visualisation):

https://user-images.githubusercontent.com/11925062/128023855-e89dccc0-9ccd-4f99-b55e-3db56545a011.mp4



https://user-images.githubusercontent.com/11925062/128043232-31578942-9951-4d31-be5d-6f2af42012d2.mp4



By restricting the set of legal (applicable) moves, we can apply the same BFS client to find optimal solutions to levels in the classic puzzle game Sokoban. In Sokoban, only straight pushes are allowed, and no pulls. Here is a visualisation of the solution found to the last level of the Sokogen variant of Sokoban, [Sokogen level 78](https://www.sokobanonline.com/play/web-archive/jacques-duthen/sokogen-990602-levels/87496_sokogen-990602-levels-78):



https://user-images.githubusercontent.com/11925062/128036105-fbf5d0a0-2cbc-4b19-ae31-a9313b218f1f.mp4



A final example is the multi-agent level below with a lot of potential for conflicts between the 4 agents, as seen in the solution of the client to the left. The client on the right on the other hand elegantly solves the conflict between the agents, and produces a faster solution (lower action cost). 




https://user-images.githubusercontent.com/11925062/128043884-608c2020-95ec-41f4-8088-170c6cf1c4df.mp4



**REFERENCES**

Roni Stern, Nathan R. Sturtevant, Ariel Felner, Sven Koenig, Hang Ma, Thayne T. Walker, Jiaoyang Li, Dor Atzmon, Liron Cohen, T. K. Satish Kumar, Roman Barták, and Eli Boyarski. Multi-agent pathfinding: Definitions, variants, and benchmarks. In _Proceedings of the 12th International Symposium on Combinatorial Search (SoCS)_,
pages 151–159, 2019.
