// import logo from './logo.svg';
// import './App.css';

// function App() {
//   return (
//     <div className="App">
//       <header className="App-header">
//         <img src={logo} className="App-logo" alt="logo" />
//         <p>
//           Edit <code>src/App.js</code> and save to reload.
//         </p>
//         <a
//           className="App-link"
//           href="https://reactjs.org"
//           target="_blank"
//           rel="noopener noreferrer"
//         >
//           Learn React
//         </a>
//       </header>
//     </div>
//   );
// }

// export default App;

import "./styles/App.css";
import Navbar from "./components/Navbar";
import HDBBackground from "./assets/hdb.png";

function App() {
  return (
    <div className="app">
      <Navbar/>

      <section className="cover">
        <div className="cover-left">
          <h1>
            Don’t just find a house.<br />
            <span className="highlight">Find a habitat.</span>
          </h1>

          <p>
            We translate your lifestyle needs into a personalized HDB compatibility score.
          </p>

          <div className="ratings">
            <div>
              <div className="stars">
                {[...Array(5)].map((_, i) => (
                  <svg
                    key={i}
                    className="star"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 17.3l-6.18 3.73 1.64-7.03L2 9.24l7.19-.61L12 2l2.81 6.63L22 9.24l-5.46 4.76 1.64 7.03z"/>
                  </svg>
                ))}
              </div>
              <p>Usefulness</p>
            </div>
            <div>
              <div className="stars">
                {[...Array(5)].map((_, i) => (
                  <svg
                    key={i}
                    className="star"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 17.3l-6.18 3.73 1.64-7.03L2 9.24l7.19-.61L12 2l2.81 6.63L22 9.24l-5.46 4.76 1.64 7.03z"/>
                  </svg>
                ))}
              </div>
              <p>Reliability</p>
            </div>
            <div>
              <div className="stars">
                {[...Array(5)].map((_, i) => (
                  <svg
                    key={i}
                    className="star"
                    viewBox="0 0 24 24"
                    fill="currentColor"
                  >
                    <path d="M12 17.3l-6.18 3.73 1.64-7.03L2 9.24l7.19-.61L12 2l2.81 6.63L22 9.24l-5.46 4.76 1.64 7.03z"/>
                  </svg>
                ))}
              </div>
              <p>Accuracy</p>
            </div>
          </div>

          <button className="primary-btn">Start Lifestyle Quiz</button>
        </div>

        <div className="cover-right">
          <img src={HDBBackground} alt="HDB Background" className="cover-image"/>
        </div>
      </section>

      <section className="features">
        <div>
          <h3>Lifestyle First</h3>
          <p>We focus on how you live, not just where you live.</p>
        </div>

        <div>
          <h3>Smart Scoring</h3>
          <p>We calculate a 0–100% match score for every block.</p>
        </div>

        <div>
          <h3>Future Vision</h3>
          <p>We warn you about future construction risks.</p>
        </div>

        <div>
          <h3>Transparent Ranking</h3>
          <p>We explain why blocks rank higher or lower.</p>
        </div>

        <div>
          <h3>Fair Commutes</h3>
          <p>We balance travel times for one or two daily commuters.</p>
        </div>
      </section>

    </div>
  );
}

export default App;
