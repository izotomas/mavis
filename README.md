# mavis-private

MAvis is short for "Multi-Agent Visualisation Tool". It is a tool to support the design, implementation and test of single- and multi-agent planning algorithms. Various discrete single-and multi-agent domains can be modelled and visualised with the tool. The instances of a given domain are called **levels**, and these are typically what is known as planning problems or planning tasks in automated planning (a given goal has to be reached from a given initial state through a sequence of actions). The tool doesn't provide any planning system for solving levels, it only provides the models of the different domains and a graphical user interface to visualise levels and animate solutions. The system for visualising levels and solutions is called the **server**. Users of the tool (e.g. students or researchers in AI) then have to provide their own AI system (planning system) for solving levels. That AI system is called a **client**. 

The original domain developed for MAvis is called the **hospital domain**. It is a multi-agent domain where agents have to move boxes into designated goal cells. Each goal cell has to be matched by a box of the same letter, and each agent can only move a box of the same color as itself. A level can also contain goal cells for the agents, hence making the domain into a generalisation of multi-agent path finding (Stern et al, 2019). The following video shows a client (Croissant) solving a level (MAAIoliMAsh) in the hospital domain: 

https://user-images.githubusercontent.com/11925062/127993622-61a5fb10-ac0f-4a3a-9de8-e314a96c2b67.mov

Both client and level are made by students attending the course 02285 Artificial Intelligence and Multi-Agent Systems at Technical University of Denmark (DTU). The MAvis tool was originally developed to be used as a teaching tool in that particular course, and the original version only supported that single domain. A detailed description of the hospital domain is available in [docs/domains/hospital/hospital_domain.pdf](docs/domains/hospital)

@@other examples




https://user-images.githubusercontent.com/11925062/127990800-6e492231-179b-4798-a9fd-c837a42a9b9e.mov



**REFERENCES**

Roni Stern, Nathan R. Sturtevant, Ariel Felner, Sven Koenig, Hang Ma, Thayne T. Walker, Jiaoyang Li, Dor Atzmon, Liron Cohen, T. K. Satish Kumar, Roman Barták, and Eli Boyarski. Multi-agent pathfinding: Definitions, variants, and benchmarks. In _Proceedings of the 12th International Symposium on Combinatorial Search (SoCS)_,
pages 151–159, 2019.
